// HearthStone/src/main/resources/static/js/game-room.js

// 전역 객체 GAME_DATA에서 모든 초기값을 가져옵니다.
const { roomNo, currentUserId, initialGameStateJson } = GAME_DATA;

// DOM 요소들을 전역 변수로 가져옵니다.
const board = document.querySelector('.game-board');
const myField = document.getElementById('my-field');
const myHand  = document.getElementById('my-hand');
const arrow   = document.getElementById('attack-arrow');
const popover = document.getElementById('card-detail-popover');

const SoundManager = {
    audioContext: null,
    soundBuffers: {},
	backgroundMusicSource: null,
    // 사운드 시스템 초기화
    init() {
        try {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            console.error("Web Audio API is not supported in this browser");
        }
    },

    // 사운드 파일 미리 불러오기
    loadSound(name, url) {
        if (!this.audioContext) return;
        
        fetch(url)
            .then(response => response.arrayBuffer())
            .then(arrayBuffer => this.audioContext.decodeAudioData(arrayBuffer))
            .then(audioBuffer => {
                this.soundBuffers[name] = audioBuffer;
                console.log(`${name} sound loaded successfully.`);
            })
            .catch(error => console.error(`Error loading sound: ${name}`, error));
    },

    // 불러온 사운드 재생하기
    playSound(name) {
        if (!this.audioContext || !this.soundBuffers[name]) {
            console.warn(`Sound not found or not loaded: ${name}`);
            return;
        }
        const source = this.audioContext.createBufferSource();
        source.buffer = this.soundBuffers[name];
        source.connect(this.audioContext.destination);
        source.start(0);
    },
	/**
	     * 배경음악을 재생하고, 반복(loop)되도록 설정합니다.
	     * @param {string} name - 불러온 배경음악의 이름
	     */
	    playBackgroundMusic(name) {
	        if (this.backgroundMusicSource) {
	            this.backgroundMusicSource.stop(); // 이미 재생 중이면 중지
	        }
	        if (!this.audioContext || !this.soundBuffers[name]) {
	            console.warn(`Background music not found: ${name}`);
	            return;
	        }
	        this.backgroundMusicSource = this.audioContext.createBufferSource();
	        this.backgroundMusicSource.buffer = this.soundBuffers[name];
	        this.backgroundMusicSource.loop = true; // ✨ 반복 재생 설정
	        this.backgroundMusicSource.connect(this.audioContext.destination);
	        this.backgroundMusicSource.start(0);
	    },

	    /**
	     * 배경음악 재생을 중지합니다.
	     */
	    stopBackgroundMusic() {
	        if (this.backgroundMusicSource) {
	            this.backgroundMusicSource.stop();
	            this.backgroundMusicSource = null;
	        }
	    }
	
};

// 상태 변수
let stompClient = null;
let gameState = null;
let detailTimer = null;
let draggingCardId = null;
let placeholderCard = null;
let currentDropIndex = -1;
let previousGameState = null;

let currentActionContext = {
    isAttacking: false,
    attackerElement: null,
    isUsingHeroPower: false
};

// 전역 변수: 상대방 ID (renderGameBoard에서 초기화)
let opponentId = null; 

// 페이지가 로드되면 게임 초기화를 시작합니다.
document.addEventListener('DOMContentLoaded', () => {
    SoundManager.init(); // ✨ 사운드 매니저 초기화
    // ✨ 필요한 사운드들을 미리 불러옵니다. (이름과 파일 경로는 예시입니다)
    SoundManager.loadSound('attack', '/sounds/attack.mp3');
    SoundManager.loadSound('minion-death', '/sounds/minion-death.wav');
    SoundManager.loadSound('card-play', '/sounds/card-play.ogg');
	SoundManager.loadSound('background', '/sounds/background.mp3');
	
    initializeGame(); // 기존 게임 초기화 함수
});

function initializeGame() {
    // ✨ [수정됨] 배경음악을 한 번만 재생하기 위한 이벤트 리스너 추가
    document.body.addEventListener('click', () => {
        // audioContext가 사용자의 클릭에 의해 활성화된 후에만 사운드 재생 가능
        if (SoundManager.audioContext && SoundManager.audioContext.state === 'suspended') {
            SoundManager.audioContext.resume();
        }
        SoundManager.playBackgroundMusic('background');
    }, { once: true }); // ✨ { once: true } 옵션으로 이 이벤트가 딱 한 번만 실행되도록 함

    try {
        if (initialGameStateJson && typeof initialGameStateJson === 'string' && initialGameStateJson.trim() !== 'null') {
            const firstState = JSON.parse(initialGameStateJson);
            renderGameBoard(null, firstState); // 초기 렌더링
            previousGameState = firstState;
            gameState = firstState;
        }
    } catch (e) {
        console.error("초기 게임 상태(JSON) 파싱 중 오류 발생:", e, "데이터:", initialGameStateJson);
        alert("게임 데이터를 불러오는 데 심각한 오류가 발생했습니다. 콘솔을 확인해주세요.");
        return;
    }
    
    board.addEventListener('click', e => {
        const tgt = e.target.closest('.targetable');
        if(currentActionContext.isAttacking){
            if(tgt) executeAttack(tgt);
            else if(!e.target.closest('.can-attack')) resetActionContext();
            return;
        }
        if(currentActionContext.isUsingHeroPower){
            if(tgt){
                const ti = tgt.dataset.targetType === 'hero' ? -1 : parseInt(tgt.dataset.targetIndex, 10);
                let targetPlayerId = null;

                if (tgt.dataset.targetType === 'hero') {
                    if (tgt.id === 'my-hero-target') {
                        targetPlayerId = currentUserId;
                    } else if (tgt.id === 'opponent-hero-target') {
                        targetPlayerId = opponentId; 
                    }
                } else if (tgt.dataset.targetType === 'minion') {
                    targetPlayerId = tgt.dataset.targetPlayerId;
                }
                
                if (targetPlayerId) {
                    sendAction({type:'USE_HERO_POWER', targetIndex:ti, targetPlayerId: targetPlayerId});
                } else {
                    console.warn("영웅 능력 대상 플레이어 ID를 결정할 수 없습니다.", tgt);
                    resetActionContext(); 
                }
            } else {
                if (!e.target.closest('.hero-power-btn')) { 
                    resetActionContext();
                }
            }
            return; 
        }
    });

    document.getElementById('endTurnButton').addEventListener('click', endTurn);
    document.getElementById('concedeButton').addEventListener('click', concede);
    
    connect();
}

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, frame => {
        document.getElementById('concedeButton').disabled = false;
        
        // 공개 채널 구독
        stompClient.subscribe(`/topic/game/${roomNo}`, msg => handleGameEvent(JSON.parse(msg.body)));
        
        // 게임 상태 업데이트 채널 구독
        stompClient.subscribe(`/topic/game/${roomNo}/state`, msg => {
            const newState = JSON.parse(msg.body);
            renderGameBoard(previousGameState, newState); // (정상 동작) 이전/새 상태 전달
            previousGameState = newState;
            gameState = newState;
        });
        
        // 개인 이벤트 채널 구독
        stompClient.subscribe('/user/queue/event', msg => handlePrivateGameEvent(JSON.parse(msg.body)));
        
        // 에러 채널 구독
        stompClient.subscribe('/user/queue/errors', msg => alert("알림: " + msg.body));

        // ✨ [수정됨] 초기 렌더링 로직
        if (gameState) {
            // 맨 처음 화면을 그릴 때, 이전 상태는 없고(null) 현재 상태만(gameState) 전달
            renderGameBoard(null, gameState);
            previousGameState = gameState;
        } else {
            // 만약 초기 상태 정보가 없으면 서버에 요청
            stompClient.send(`/app/game/${roomNo}/requestState`, {}, {});
        }
    });
}

function sendAction(action) {
    if (!stompClient || !stompClient.connected) {
        console.error("WebSocket is not connected.");
        return;
    }

    if (action.type === 'ATTACK') {
        const attackerElement = document.querySelector(`#my-field .card[data-index="${action.attackerIndex}"]`);
        const targetElement = action.targetIndex === -1 
            ? document.getElementById('opponent-hero-target')
            : document.querySelector(`#opponent-field .card[data-index="${action.targetIndex}"]`);

        if (attackerElement && targetElement) {
			SoundManager.playSound('attack');
            // ✨ [수정됨] 애니메이션을 위한 '복제본' 생성
            const boardRect = document.querySelector('.game-board').getBoundingClientRect();
            const attackerRect = attackerElement.getBoundingClientRect();
            const targetRect = targetElement.getBoundingClientRect();
            
            // 1. 복제본(clone) 생성 및 스타일 설정
            const clone = attackerElement.cloneNode(true);
            clone.style.position = 'absolute';
            clone.style.left = `${attackerRect.left - boardRect.left}px`;
            clone.style.top = `${attackerRect.top - boardRect.top}px`;
            clone.style.width = `${attackerRect.width}px`;
            clone.style.height = `${attackerRect.height}px`;
            clone.style.zIndex = '1000';
            clone.style.pointerEvents = 'none'; // 복제본이 마우스 이벤트를 방해하지 않도록 함
            document.querySelector('.game-board').appendChild(clone);

            // 2. 원래 요소는 잠시 숨김
            attackerElement.style.opacity = '0';

            // 3. 복제본으로 애니메이션 실행
            gsap.to(clone, {
                duration: 0.3,
                left: `${targetRect.left - boardRect.left + (targetRect.width - attackerRect.width) / 2}px`,
                top: `${targetRect.top - boardRect.top + (targetRect.height - attackerRect.height) / 2}px`,
                scale: 1.1,
                ease: "power1.inOut",
                onComplete: () => {
                    // 공격이 부딪히는 느낌을 주기 위해 아주 짧은 딜레이 후 복제본 제거
                    setTimeout(() => {
                        clone.remove();
                        // 4. 서버 응답이 오면 renderGameBoard가 호출되면서 원래 요소의 opacity가 1로 돌아옴
                        // 만약을 위해 명시적으로 복구 코드를 둘 수도 있습니다.
                        attackerElement.style.opacity = '1';
                    }, 100);
                }
            });
        }
    }

    action.playerId = currentUserId;
    stompClient.send(`/app/game/${roomNo}/action`, {}, JSON.stringify(action));

    resetActionContext();
}

function endTurn() { sendAction({ type: 'END_TURN' }); }
function concede() { if (confirm("정말로 항복하시겠습니까?")) sendAction({ type: 'CONCEDE' }); }

function renderGameBoard(oldState, newState) {
    // 최초 렌더링이거나 상태가 없으면 애니메이션 없이 바로 렌더링
    if (!oldState || !newState) {
        if (newState) {
            gameState = newState; // gameState 업데이트
            opponentId = Object.keys(newState.players).find(id => id !== currentUserId);
            renderUI(newState);
        }
        return;
    }

    const changes = {
        damages: [],
        deaths: []
    };

    // 상태를 비교하여 변경점(데미지, 죽음) 찾기
    for (const playerId in newState.players) {
        const oldPlayer = oldState.players[playerId];
        const newPlayer = newState.players[playerId];

        // 필드 하수인 비교
        newPlayer.board.forEach((newMinion, index) => {
            const oldMinion = oldPlayer.board.find(m => m.cardDTO.no === newMinion.cardDTO.no && m.cardDTO.name === newMinion.cardDTO.name);
            if (oldMinion && oldMinion.currentHealth > newMinion.currentHealth) {
                changes.damages.push({
                    targetId: `${playerId}-minion-${index}`,
                    amount: newMinion.currentHealth - oldMinion.currentHealth
                });
            }
        });
        
        // 죽은 하수인 찾기
        oldPlayer.board.forEach(oldMinion => {
            const stillAlive = newPlayer.board.some(newMinion => newMinion.cardDTO.no === oldMinion.cardDTO.no && newMinion.cardDTO.name === oldMinion.cardDTO.name);
            if (!stillAlive) {
                changes.deaths.push({ 
                    playerId: playerId,
                    cardId: oldMinion.cardDTO.no,
                    cardName: oldMinion.cardDTO.name
                });
            }
        });

        // 영웅 데미지 비교
        if (oldPlayer.health > newPlayer.health) {
            changes.damages.push({
                targetId: `${playerId}-hero`,
                amount: newPlayer.health - oldPlayer.health
            });
        }
    }

    // 변경점이 있으면 전투 애니메이션 실행, 없으면 바로 UI 렌더링
    if (changes.damages.length > 0 || changes.deaths.length > 0) {
        animateCombat(changes, newState);
    } else {
        renderUI(newState);
    }
}

// createCardDiv 함수를 이곳으로 이동합니다.
function createCardDiv(card) {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.backgroundImage = `url(${card.imageUrl})`;
    div.innerHTML = `<div class="card-cost">${card.cost}</div><div class="card-stats"><span class="card-attack">${card.attack||0}</span><span class="card-health">${card.health}</span></div>`;
    div.dataset.cardId = card.no;
    div.dataset.name = card.name;
    div.dataset.cost = card.cost;
    div.dataset.attack = card.attack||0;
    div.dataset.health = card.health;
    div.dataset.description = card.description||'효과 없음';
    div.dataset.keywords = card.keyword||'';
    div.addEventListener('mouseenter', e => {
        if (!currentActionContext.isAttacking) detailTimer = setTimeout(() => showCardDetail(e.target), 800);
    });
    div.addEventListener('mouseleave', () => { clearTimeout(detailTimer); hideCardDetail(); });
    return div;
}

// createMinionDiv 함수를 이곳으로 이동합니다.
function createMinionDiv(minion, index, canAttack, isTarget) { // isTarget은 상대방 하수인일 때 true
    // minion.cardVO 대신 minion.cardDTO를 사용하도록 수정
    const div = createCardDiv(minion.cardDTO); 
    div.dataset.index = index;
    div.dataset.health = minion.currentHealth;
    div.querySelector('.card-health').textContent = minion.currentHealth;
    if (canAttack) { 
        div.classList.add('can-attack'); 
        div.addEventListener('click', e => {
            e.stopPropagation(); 
            startAttack(div);
        });
    }
    
    div.classList.add('targetable'); 
    div.dataset.targetType='minion'; 
    div.dataset.targetIndex=index;
    // minionDiv.dataset.targetPlayerId는 renderPlayerUI에서 설정됩니다.

    if (isTarget) { 
        div.classList.add('enemy-target'); 
    } else { 
        div.classList.add('friendly-target'); 
    }
    return div;
}


function renderPlayerUI(prefix, playerState, isMyTurn) {
    const isMyArea = prefix === 'my';
    
    document.getElementById(`${prefix}-health`).textContent = playerState.health;
    document.getElementById(`${prefix}-mana`).textContent = `${playerState.mana} / ${playerState.maxMana}`;
    document.getElementById(`${prefix}-deck-count`).textContent = playerState.deckSize;

    const heroImageEl = document.getElementById(`${prefix}-hero-image`);
    if (playerState.hero && playerState.hero.imageUrl) {
        heroImageEl.src = playerState.hero.imageUrl;
    }
    
    const heroTargetDiv = document.getElementById(`${prefix}-hero-target`);
    if (heroTargetDiv) {
        heroTargetDiv.classList.add('targetable');
        heroTargetDiv.classList.toggle('friendly-target', isMyArea);
        heroTargetDiv.classList.toggle('enemy-target', !isMyArea);
        heroTargetDiv.dataset.targetType = 'hero';
        heroTargetDiv.dataset.targetIndex = -1;
        heroTargetDiv.dataset.targetPlayerId = playerState.playerId;
    }

    const heroPowerBtn = document.getElementById(`${prefix}-hero-power`);
    heroPowerBtn.innerHTML = `<strong>${playerState.hero.heroPowerName}</strong><span>(${playerState.hero.heroPowerCost})</span>`;
    const canUseHeroPower = isMyTurn && playerState.mana >= playerState.hero.heroPowerCost && !playerState.hasUsedHeroPowerThisTurn;
    heroPowerBtn.disabled = !canUseHeroPower;

    heroPowerBtn.addEventListener('mouseenter', () => {
        clearTimeout(detailTimer);
        detailTimer = setTimeout(() => showHeroPowerDetail(heroPowerBtn, playerState.hero), 800);
    });
    heroPowerBtn.addEventListener('mouseleave', () => {
        clearTimeout(detailTimer);
        hideCardDetail(); 
    });

    if (!heroPowerBtn.disabled) {
        heroPowerBtn.onclick = () => {
            resetActionContext(); 
            const heroClassName = playerState.hero.className; 
            if (heroClassName === "사냥꾼" || heroClassName === "흑마법사") { 
                if (heroClassName === "사냥꾼") {
                    sendAction({ type: 'USE_HERO_POWER', targetIndex: -1, targetPlayerId: opponentId });
                } else if (heroClassName === "흑마법사") {
                    sendAction({ type: 'USE_HERO_POWER', targetIndex: -1, targetPlayerId: currentUserId });
                }
            } else if (heroClassName === "사제") {
                currentActionContext.isUsingHeroPower = true;
                highlightTargets(true, 'friendly'); 
                heroPowerBtn.classList.add('targeting');
                currentActionContext.attackerElement = heroPowerBtn;
                arrow.style.display = 'block';
                board.addEventListener('mousemove', updateArrowPosition);
                const r = heroPowerBtn.getBoundingClientRect();
                updateArrowPosition({ clientX: r.left + r.width / 2, clientY: r.top + r.height / 2 });
            } else {
                currentActionContext.isUsingHeroPower = true;
                highlightTargets(true, 'all'); 
                heroPowerBtn.classList.add('targeting');
                currentActionContext.attackerElement = heroPowerBtn;
                arrow.style.display = 'block';
                board.addEventListener('mousemove', updateArrowPosition);
                const r = heroPowerBtn.getBoundingClientRect();
                updateArrowPosition({ clientX: r.left + r.width / 2, clientY: r.top + r.height / 2 });
            }
        };
    } else {
        heroPowerBtn.onclick = null;
    }

    const fieldDiv = document.getElementById(`${prefix}-field`);
    fieldDiv.innerHTML = '';
    
    playerState.board.forEach((minion, idx) => {
        const minionDiv = createMinionDiv(minion, idx, isMyArea && isMyTurn && minion.canAttack && !minion.hasAttackedThisTurn, !isMyArea);
        minionDiv.dataset.targetPlayerId = playerState.playerId;
        fieldDiv.appendChild(minionDiv);
    });

    if (isMyArea) {
        const myHandDiv = document.getElementById('my-hand');
        myHandDiv.innerHTML = '';
        myHandDiv.style.gap = '10px';
        playerState.hand.forEach((cardData, index) => { // ✨ 인덱스 변수 'index' 추가
            const cardDiv = createCardDiv(cardData);
            if (isMyTurn && playerState.mana >= cardData.cost) {
                cardDiv.classList.add('can-play');
                cardDiv.draggable = true;
                cardDiv.addEventListener('dragstart', dragCardFromHand);
                cardDiv.addEventListener('dragend', handleDragEnd);
            }
            
            // ✨ [추가됨] 카드에 마우스를 올리거나 내렸을 때 서버로 이벤트를 보냅니다.
            cardDiv.addEventListener('mouseenter', () => {
                sendAction({ type: 'HOVER_CARD', insertIndex: index });
            });
            cardDiv.addEventListener('mouseleave', () => {
                sendAction({ type: 'HOVER_CARD', insertIndex: -1 }); // -1을 보내 호버 종료를 알림
            });

            myHandDiv.appendChild(cardDiv);
        });
    } else {
        const opponentHandDiv = document.getElementById('opponent-hand');
        opponentHandDiv.innerHTML = '';
        for (let i = 0; i < playerState.hand.length; i++) {
            const cardBackDiv = document.createElement('div');
            cardBackDiv.className = 'card card-back';
            cardBackDiv.dataset.cardIndex = i;
            opponentHandDiv.appendChild(cardBackDiv);
        }
    }
}

function dragCardFromHand(ev) {
    draggingCardId = ev.target.dataset.cardId;
    ev.dataTransfer.effectAllowed = 'move';
    clearTimeout(detailTimer);
    hideCardDetail();
}

function handleDragEnd(ev) {
    if (placeholderCard) {
        placeholderCard.remove();
        placeholderCard = null;
    }
    draggingCardId = null;
    currentDropIndex = -1;
}

function handleDragOverField(ev) {
    ev.preventDefault();
    if (!draggingCardId) return;

    const mouseX = ev.clientX;
    const minions = Array.from(myField.querySelectorAll('.card:not(.placeholder)'));
    let insertIndex = minions.length;

    for (let i = 0; i < minions.length; i++) {
        const minionRect = minions[i].getBoundingClientRect();
        if (mouseX < minionRect.left + minionRect.width / 2) {
            insertIndex = i;
            break;
        }
    }

    if (insertIndex === currentDropIndex) return;
    currentDropIndex = insertIndex;

    if (!placeholderCard) {
        placeholderCard = document.createElement('div');
        placeholderCard.className = 'card placeholder';
    }

    if (insertIndex >= minions.length) {
        myField.appendChild(placeholderCard);
    } else {
        myField.insertBefore(placeholderCard, minions[insertIndex]);
    }
}

function handleDragLeaveField(ev) {
    const fieldRect = myField.getBoundingClientRect();
    if (ev.clientX < fieldRect.left || ev.clientX > fieldRect.right || ev.clientY < fieldRect.top || ev.clientY > fieldRect.bottom) {
        if (placeholderCard) {
            placeholderCard.remove();
            placeholderCard = null;
        }
        currentDropIndex = -1;
    }
}

function dropOnMyField(ev) {
    ev.preventDefault();
    if (!draggingCardId) return;

    if (placeholderCard) {
        placeholderCard.remove();
        placeholderCard = null;
    }

    sendAction({ 
        type: 'PLAY_CARD', 
        cardId: parseInt(draggingCardId), 
        insertIndex: currentDropIndex
    });

    draggingCardId = null;
    currentDropIndex = -1;
}

function resetActionContext(){
    if(currentActionContext.attackerElement) currentActionContext.attackerElement.classList.remove('is-attacking');
    currentActionContext.isAttacking = false;
    currentActionContext.attackerElement = null;
    currentActionContext.isUsingHeroPower = false;
    arrow.style.display='none';
    highlightTargets(false, 'all'); 
    const heroPowerBtn = document.getElementById('my-hero-power');
    if (heroPowerBtn) heroPowerBtn.classList.remove('targeting');
    board.removeEventListener('mousemove',updateArrowPosition);
}

// highlightTargets 함수를 mode 파라미터를 받도록 수정합니다.
function highlightTargets(active, mode = 'all') { 
    document.querySelectorAll('.targetable').forEach(el => {
        const isFriendlyTarget = el.classList.contains('friendly-target');
        const isEnemyTarget = el.classList.contains('enemy-target');

        let shouldHighlight = false;
        if (mode === 'all') { 
            shouldHighlight = true;
        } else if (mode === 'friendly' && isFriendlyTarget) { 
            shouldHighlight = true;
        } else if (mode === 'enemy' && isEnemyTarget) { 
            shouldHighlight = true;
        }

        el.classList.toggle('targeting-active', active && shouldHighlight);
    });
}

function startAttack(el){
    if(currentActionContext.isUsingHeroPower||(currentActionContext.isAttacking&&currentActionContext.attackerElement===el)){
        resetActionContext(); return;
    }
    resetActionContext();
    currentActionContext.isAttacking=true; currentActionContext.attackerElement=el;
    el.classList.add('is-attacking'); highlightTargets(true, 'enemy'); 
    arrow.style.display='block'; board.addEventListener('mousemove',updateArrowPosition);
    const r=el.getBoundingClientRect(); updateArrowPosition({clientX:r.left+r.width/2,clientY:r.top});
}

function updateArrowPosition(e){
    if(!currentActionContext.attackerElement) return;
    const ra=currentActionContext.attackerElement.getBoundingClientRect(),
        br=board.getBoundingClientRect(),
        sx=ra.left-br.left+ra.width/2, sy=ra.top-br.top+ra.height/2,
        ex=e.clientX-br.left, ey=e.clientY-br.top,
        d=Math.hypot(ex-sx,ey-sy), ang=Math.atan2(ey-sy,ex-sx)*180/Math.PI;
    arrow.style.left=`${sx}px`; arrow.style.top=`${sy}px`;
    arrow.style.width=`${d}px`; arrow.style.transform=`rotate(${ang}deg)`;
}

function executeAttack(target){
    if(!currentActionContext.isAttacking) return;
    const ai=parseInt(currentActionContext.attackerElement.dataset.index,10),
        ti=target.dataset.targetType==='minion'?parseInt(target.dataset.targetIndex,10):-1;
    sendAction({type:'ATTACK',attackerIndex:ai,targetIndex:ti});
}

function handleGameEvent(ev){
    if(ev.type==='GAME_OVER'){
        const w = ev.winner === currentUserId ? '승리' : '패배';
        document.getElementById('turn-info').innerHTML = `<h2>게임 종료! [${w}]</h2><p>사유: ${ev.reason}</p>`;
        
        // 기존 버튼 숨기기
        document.getElementById('endTurnButton').style.display = 'none';
        document.getElementById('concedeButton').style.display = 'none';

        // *** 핵심 수정: 새로운 버튼 그룹을 표시합니다. ***
        const gameOverControls = document.getElementById('gameOverControls');
        const playAgainSameDeckButton = document.getElementById('playAgainSameDeckButton');
        
        // 현재 플레이어의 덱 ID를 가져옵니다. (gameState에서)
        const myDeckId = gameState.players[currentUserId].deckId; 
        
        // "같은 덱으로 다시하기" 버튼에 클릭 이벤트를 설정합니다.
        if(myDeckId) {
            playAgainSameDeckButton.onclick = () => {
                window.location.href = `/game/match?deckId=${myDeckId}`;
            };
        } else {
            // 혹시 모를 예외 상황 처리
            playAgainSameDeckButton.disabled = true;
        }

        gameOverControls.style.display = 'inline-block';
        resetActionContext();
    }
}

// NEW: 영웅 능력 상세 정보를 표시하는 함수 추가
function showHeroPowerDetail(el, heroData) {
    popover.querySelector('#popover-name').textContent = heroData.heroPowerName;
    popover.querySelector('#popover-cost').textContent = heroData.heroPowerCost;
    
    // 영웅 능력에는 공격력, 생명력, 키워드가 없으므로 필드를 비웁니다.
    popover.querySelector('#popover-attack').textContent = '';
    popover.querySelector('#popover-health').textContent = '';
    popover.querySelector('#popover-keywords').textContent = '';

    popover.querySelector('#popover-description').textContent = heroData.heroPowerDescription || '효과 없음';

    // 팝오버 위치 설정
    const r = el.getBoundingClientRect();
    popover.style.left = `${r.right + 10}px`;
    popover.style.top = `${r.top}px`;
    popover.style.display = 'block';
}

function showCardDetail(el){
    popover.querySelector('#popover-name').textContent = el.dataset.name;
    popover.querySelector('#popover-cost').textContent = el.dataset.cost;
    popover.querySelector('#popover-attack').textContent = el.dataset.attack;
    popover.querySelector('#popover-health').textContent = el.dataset.health;
    popover.querySelector('#popover-description').textContent = el.dataset.description;
    popover.querySelector('#popover-keywords').textContent = el.dataset.keywords.replace(/,/g,', ');
    const r=el.getBoundingClientRect();
    popover.style.left=`${r.right+10}px`; popover.style.top=`${r.top}px`;
    popover.style.display='block';
}

function hideCardDetail(){ popover.style.display='none'; }

function handleOpponentCardHover(cardIndex, isHovering) {
    const opponentHandDiv = document.getElementById('opponent-hand');
    if (!opponentHandDiv) return;

    const cardToAnimate = opponentHandDiv.querySelector(`.card[data-card-index="${cardIndex}"]`);
    
    if (cardToAnimate) {
        if (isHovering) {
            cardToAnimate.style.transform = 'translateY(20px)';
        } else {
            cardToAnimate.style.transform = 'translateY(0)';
        }
    }
}
// game-room.js 파일 맨 아래에 추가
function handlePrivateGameEvent(event) {
    if (event.type === 'OPPONENT_CARD_HOVER') {
        const cardIndex = event.cardIndex;
        const isHovering = cardIndex !== -1;
        handleOpponentCardHover(cardIndex, isHovering);
    }
}

/**
 * 전투 애니메이션(데미지, 죽음)을 순차적으로 실행하는 함수
 * @param {object} changes - 데미지 및 죽음 정보
 * @param {object} finalState - 애니메이션 후 최종 게임 상태
 */
function animateCombat(changes, finalState) {
    const timeline = gsap.timeline({
        onComplete: () => {
            // 모든 애니메이션이 끝난 후, 최종 상태로 UI를 완벽하게 렌더링
            renderUI(finalState);
        }
    });

    // 1. 데미지 애니메이션 추가
    if (changes.damages.length > 0) {
        timeline.add(() => {
            changes.damages.forEach(dmg => {
                let targetElement;
                if (dmg.targetId.includes('hero')) {
                    const heroPlayerId = dmg.targetId.split('-')[0];
                    targetElement = document.getElementById(`${heroPlayerId === currentUserId ? 'my' : 'opponent'}-hero-target`);
                } else {
                    const [playerId, type, index] = dmg.targetId.split('-');
                    const fieldId = playerId === currentUserId ? 'my-field' : 'opponent-field';
                    targetElement = document.querySelector(`#${fieldId} .card[data-index="${index}"]`);
                }

                if (targetElement) {
                    const rect = targetElement.getBoundingClientRect();
                    const damageEl = document.createElement('div');
                    damageEl.className = 'damage-text';
                    damageEl.textContent = dmg.amount;
                    document.querySelector('.game-board').appendChild(damageEl);

                    gsap.fromTo(damageEl, 
                        { left: `${rect.left + rect.width / 2}px`, top: `${rect.top + rect.height / 2}px`, opacity: 1, scale: 0.5 },
                        { duration: 1.5, top: `-=50px`, opacity: 0, scale: 1.5, ease: "power1.out", onComplete: () => damageEl.remove() }
                    );
                }
            });
        }, ">"); // 이전 애니메이션과 동시에 시작
    }

    // 2. 죽음 애니메이션 추가 (데미지 표시 후 0.5초 뒤에 실행)
    if (changes.deaths.length > 0) {
        timeline.add(() => {
            changes.deaths.forEach(death => {
                const fieldId = death.playerId === currentUserId ? 'my-field' : 'opponent-field';
                // 죽은 하수인을 이름과 카-드 ID로 다시 찾아냄
                const deadMinionElement = Array.from(document.querySelectorAll(`#${fieldId} .card`)).find(el => el.dataset.name === death.cardName && el.dataset.cardId == death.cardId);

                if (deadMinionElement) {
					SoundManager.playSound('minion-death');
                    deadMinionElement.classList.add('is-dying');
                }
            });
        }, "+=0.5");
    }
}

/**
 * 순수하게 게임 상태를 받아서 화면에 UI를 그리는 함수
 * @param {object} stateToRender - 화면에 그릴 게임 상태
 */
function renderUI(stateToRender) {
    if (!stateToRender) return;
    
    gameState = stateToRender; // 전역 gameState 업데이트
    const isMyTurn = stateToRender.currentTurnPlayerId === currentUserId;
    opponentId = Object.keys(stateToRender.players).find(id => id !== currentUserId);

    if (!opponentId) {
        console.warn("Opponent not found in state, UI render might be incomplete.");
        return;
    }

    const myState = stateToRender.players[currentUserId];
    const opponentState = stateToRender.players[opponentId];

    document.getElementById('turn-info').textContent = isMyTurn ? "내 턴입니다." : `상대방(${opponentId}) 턴입니다...`;
    const endTurnButton = document.getElementById('endTurnButton');
    endTurnButton.disabled = !isMyTurn;

    if(isMyTurn) endTurnButton.classList.add('my-turn-button-glow');
    else endTurnButton.classList.remove('my-turn-button-glow');
    
    // 이전에 만들었던 renderPlayerUI 함수를 호출하여 각 플레이어 영역을 그림
    renderPlayerUI('my', myState, isMyTurn);
    renderPlayerUI('opponent', opponentState, false);
}