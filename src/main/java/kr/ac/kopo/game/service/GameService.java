package kr.ac.kopo.game.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.ac.kopo.card.repository.CardAbilityRepository;
import kr.ac.kopo.card.repository.CardRepository;
import kr.ac.kopo.card.repository.HeroRepository;
import kr.ac.kopo.card.vo.CardAbilityEntity;
import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.card.vo.HeroVO;
import kr.ac.kopo.deck.service.DeckService;
import kr.ac.kopo.game.repository.GameRoomRepository;
import kr.ac.kopo.game.vo.CardDTO;
import kr.ac.kopo.game.vo.DeckDTO;
import kr.ac.kopo.game.vo.GameRoomEntity;
import kr.ac.kopo.game.vo.GameState;
import kr.ac.kopo.game.vo.GameStateDTO;
import kr.ac.kopo.game.vo.Minion;
import kr.ac.kopo.game.vo.PlayerState;
import kr.ac.kopo.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GameService {

    private final Map<Integer, GameState> activeGames = new ConcurrentHashMap<>();

    @Autowired private CardRepository cardRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GameRoomRepository roomRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private HeroRepository heroRepository;
    @Autowired private DeckService deckService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CardAbilityRepository cardAbilityRepository;

    public void initializeGame(GameRoomEntity room) {
        if (isGameActive(room.getNo())) {
            log.warn("이미 진행 중인 게임(방 번호: {})에 대해 초기화 시도가 있었습니다.", room.getNo());
            return;
        }
        
        if (room.getPlayer1DeckId() == null || room.getPlayer2DeckId() == null) {
            log.error("Deck ID가 null입니다! 게임을 시작할 수 없습니다. P1 Deck: {}, P2 Deck: {}", room.getPlayer1DeckId(), room.getPlayer2DeckId());
            endGame(room.getNo(), room.getPlayer2(), room.getPlayer1(), "상대방의 덱 정보 오류");
            return;
        }

        GameState gameState = new GameState();
        gameState.setGameId(String.valueOf(room.getNo()));
        
        DeckDTO player1Deck = deckService.getDeckWithCards(room.getPlayer1DeckId());
        DeckDTO player2Deck = deckService.getDeckWithCards(room.getPlayer2DeckId());

        if(player1Deck == null || player2Deck == null) {
            log.error("플레이어의 덱 정보를 찾을 수 없어 게임을 시작할 수 없습니다. Room: {}", room.getNo());
            return;
        }

        PlayerState player1 = createPlayerState(room.getPlayer1(), player1Deck);
        PlayerState player2 = createPlayerState(room.getPlayer2(), player2Deck);

        Random random = new Random();
        PlayerState firstPlayer = random.nextBoolean() ? player1 : player2;
        PlayerState secondPlayer = (firstPlayer == player1) ? player2 : player1;

        drawCards(firstPlayer, 3);
        drawCards(secondPlayer, 4);
        
        // ✨ [수정됨] 로직 순서 변경
        // 1. 게임 상태를 맵(activeGames)에 먼저 등록합니다.
        gameState.setPlayers(Map.of(player1.getPlayerId(), player1, player2.getPlayerId(), player2));
        gameState.setCurrentTurnPlayerId(firstPlayer.getPlayerId());
        gameState.setTurnCount(1);
        activeGames.put(room.getNo(), gameState);

        // 2. 그 후에 첫 턴 시작 로직을 호출합니다.
        // 이제 startNewTurnFor 내부에서 executeAbilities를 호출해도 GameState를 찾을 수 있습니다.
        startNewTurnFor(room.getNo(), firstPlayer); 

        messagingTemplate.convertAndSend("/topic/game/" + room.getNo(), Map.of("type", "GAME_START", "roomNo", room.getNo()));
    }

    private PlayerState createPlayerState(String playerId, DeckDTO deck) { 
        PlayerState playerState = new PlayerState();
        playerState.setPlayerId(playerId);
        
        playerState.setDeckId(deck.getNo());

        String heroClassName = deck.getName().replaceAll(" 덱$", "");
        HeroVO hero = heroRepository.findByClassName(heroClassName).stream().findFirst().orElse(null);
        playerState.setHero(hero);
        
        List<CardDTO> deckCards = new ArrayList<>(deck.getCards()); 
        Collections.shuffle(deckCards);
        playerState.setDeck(deckCards); 
        
        playerState.setHand(new ArrayList<CardDTO>());
        
        playerState.setBoard(new ArrayList<>()); 
        return playerState;
    }

    private void startNewTurnFor(int roomNo, PlayerState player) { 
        if (player.getMaxMana() < 10) {
            player.setMaxMana(player.getMaxMana() + 1);
        }
        player.setMana(player.getMaxMana());
        drawCards(player, 1);
        player.getBoard().forEach(minion -> {
            minion.setCanAttack(true);
            minion.setHasAttackedThisTurn(false);
        });
        player.setHasUsedHeroPowerThisTurn(false);
        player.setHeroAttackedThisTurn(false);
        player.setHeroAttack(0);

        executeAbilities(roomNo, player.getPlayerId(), "ON_TURN_START", null, null); 
    }

    public void endTurn(int roomNo, String playerId) {
        GameState gameState = activeGames.get(roomNo);
        if (gameState == null || !gameState.getCurrentTurnPlayerId().equals(playerId)) {
            sendErrorToPlayer(playerId, "지금은 내 턴이 아닙니다.");
            return;
        }
        
        PlayerState currentPlayerState = gameState.getPlayers().get(playerId);
        currentPlayerState.getBoard().forEach(minion -> minion.setHasAttackedThisTurn(false));
        currentPlayerState.setHasUsedHeroPowerThisTurn(false);
        currentPlayerState.setHeroAttackedThisTurn(false);
        currentPlayerState.setHeroAttack(0);

        executeAbilities(roomNo, currentPlayerState.getPlayerId(), "ON_TURN_END", null, null); 
        
        String opponentId = getOpponentPlayerState(gameState, playerId).getPlayerId();

        if (opponentId != null) {
            gameState.setTurnCount(gameState.getTurnCount() + 1);
            gameState.setCurrentTurnPlayerId(opponentId);
            startNewTurnFor(roomNo, gameState.getPlayers().get(opponentId)); 
            broadcastGameState(roomNo);
        }
    }

    public void playCard(int roomNo, String playerId, int cardId, int insertIndex) {
        GameState gameState = activeGames.get(roomNo);
        if (gameState == null || !gameState.getCurrentTurnPlayerId().equals(playerId)) {
            sendErrorToPlayer(playerId, "지금은 내 턴이 아닙니다.");
            return;
        }

        PlayerState playerState = gameState.getPlayers().get(playerId);
        Optional<CardDTO> cardToPlayOpt = playerState.getHand().stream()
                .filter(card -> card.getNo() == cardId).findFirst();

        if (cardToPlayOpt.isEmpty()) {
            sendErrorToPlayer(playerId, "손에 없는 카드입니다.");
            return;
        }

        CardDTO cardToPlay = cardToPlayOpt.get(); 
        if (playerState.getMana() < cardToPlay.getCost()) {
            sendErrorToPlayer(playerId, "마나가 부족합니다.");
            return;
        }
        
        playerState.setMana(playerState.getMana() - cardToPlay.getCost());
        playerState.getHand().remove(cardToPlay);
        
        List<Minion> board = playerState.getBoard();
        if (insertIndex < 0 || insertIndex > board.size()) {
            board.add(new Minion(cardToPlay));
        } else {
            board.add(insertIndex, new Minion(cardToPlay));
        }

        executeAbilities(roomNo, playerId, "ON_PLAY", cardToPlay, null); 

        if ("주문".equals(cardToPlay.getType())) {
            executeAbilities(roomNo, playerId, "ON_ANY_SPELL_CAST", cardToPlay, null); 
            executeAbilities(roomNo, playerId, "ON_MY_SPELL_CAST", cardToPlay, null); 
            
            PlayerState opponentState = getOpponentPlayerState(gameState, playerId);
            if (opponentState != null) {
                executeAbilities(roomNo, opponentState.getPlayerId(), "ON_OPPONENT_SPELL_CAST", cardToPlay, null);
            }
        }

        broadcastGameState(roomNo);
    }

    public void attack(int roomNo, String playerId, int attackerIndex, int targetIndex) {
        log.debug("[ATTACK] Room: {}, Player: {}, AttackerIndex: {}, TargetIndex: {}", roomNo, playerId, attackerIndex, targetIndex);

        GameState gameState = activeGames.get(roomNo);
        if (gameState == null) {
            sendErrorToPlayer(playerId, "게임 상태를 찾을 수 없어 공격할 수 없습니다.");
            log.warn("[ATTACK] GameState not found for room {}", roomNo);
            return;
        }
        if (!gameState.getCurrentTurnPlayerId().equals(playerId)) {
            sendErrorToPlayer(playerId, "지금은 내 턴이 아닙니다.");
            return;
        }
    
        PlayerState attackerPlayerState = gameState.getPlayers().get(playerId);
        if (attackerPlayerState == null) {
            sendErrorToPlayer(playerId, "공격자 플레이어 상태를 찾을 수 없습니다.");
            log.error("[ATTACK] Attacker PlayerState not found for ID: {}", playerId);
            return;
        }

        PlayerState defenderPlayerState = getOpponentPlayerState(gameState, playerId);
    
        if (defenderPlayerState == null) {
            sendErrorToPlayer(playerId, "수비자 플레이어 상태를 찾을 수 없습니다.");
            log.error("[ATTACK] Defender PlayerState not found for room {}", roomNo);
            return; 
        }
    
        boolean hasTauntMinions = defenderPlayerState.getBoard().stream().anyMatch(Minion::hasTaunt);
        if (hasTauntMinions) {
            if (targetIndex == -1 || (targetIndex >= defenderPlayerState.getBoard().size() || !defenderPlayerState.getBoard().get(targetIndex).hasTaunt())) {
                sendErrorToPlayer(playerId, "도발 능력을 가진 하수인을 먼저 공격해야 합니다.");
                log.info("[ATTACK] Taunt minion exists, but target is not taunt or hero. Attacker: {}, Target: {}", attackerIndex, targetIndex);
                return;
            }
        }
    
        Minion attackerMinion = null;
        // 영웅 공격인지 하수인 공격인지 구분
        if (attackerIndex == -1) { // 영웅이 공격
            log.debug("[ATTACK] Hero attacking. HeroAttack: {}, HeroAttackedThisTurn: {}", attackerPlayerState.getHeroAttack(), attackerPlayerState.isHeroAttackedThisTurn());
            if (attackerPlayerState.getHeroAttack() <= 0 || attackerPlayerState.isHeroAttackedThisTurn()) {
                sendErrorToPlayer(playerId, "영웅은 공격할 수 없습니다.");
                return;
            }
            attackerPlayerState.setHeroAttackedThisTurn(true);
            int heroAttack = attackerPlayerState.getHeroAttack();

            // 대상에 피해 적용
            if (targetIndex == -1) { // 상대 영웅 대상
                log.debug("[ATTACK] Hero attacking enemy hero. Damage: {}", heroAttack);
                defenderPlayerState.setHealth(defenderPlayerState.getHealth() - heroAttack);
            } else if (targetIndex < defenderPlayerState.getBoard().size()) { // 상대 하수인 대상
                Minion targetMinion = defenderPlayerState.getBoard().get(targetIndex);
                log.debug("[ATTACK] Hero attacking enemy minion. HeroDamage: {}, MinionAttack: {}", heroAttack, targetMinion.getCardDTO().getAttack());
                targetMinion.takeDamage(heroAttack);
            }
        } else { // 하수인이 공격
            if (attackerIndex >= attackerPlayerState.getBoard().size()) return; // 유효하지 않은 공격자 인덱스
            attackerMinion = attackerPlayerState.getBoard().get(attackerIndex);
            if (!attackerMinion.isCanAttack() || attackerMinion.isHasAttackedThisTurn()) {
                sendErrorToPlayer(playerId, "그 하수인은 공격할 수 없습니다.");
                return;
            }
            attackerMinion.setHasAttackedThisTurn(true);
            int minionAttack = attackerMinion.getCardDTO().getAttack(); 

            // 대상에 피해 적용
            if (targetIndex == -1) { // 상대 영웅 대상
                log.debug("[ATTACK] Minion attacking enemy hero. Minion: {}, Damage: {}", attackerMinion.getCardDTO().getName(), minionAttack); 
                defenderPlayerState.setHealth(defenderPlayerState.getHealth() - minionAttack);
            } else if (targetIndex < defenderPlayerState.getBoard().size()) { // 상대 하수인 대상
                Minion targetMinion = defenderPlayerState.getBoard().get(targetIndex);
                log.debug("[ATTACK] Minion attacking enemy minion. Attacker: {}, Defender: {}, AttackerDamage: {}, DefenderDamage: {}",
                          attackerMinion.getCardDTO().getName(), targetMinion.getCardDTO().getName(), minionAttack, targetMinion.getCardDTO().getAttack()); 
                
                // 쌍방 피해
                attackerMinion.takeDamage(targetMinion.getCardDTO().getAttack()); 
                targetMinion.takeDamage(minionAttack);
            }
            attackerPlayerState.getBoard().removeIf(Minion::isDead);
        }
    
        // 공격 후 죽은 하수인 확인 (ON_MINION_DEATH 능력 발동을 위해)
        // 보드에서 제거되기 전에 상태를 캡처
        List<Minion> attackerMinionsBeforeRemoval = new ArrayList<>(attackerPlayerState.getBoard());
        List<Minion> defenderMinionsBeforeRemoval = new ArrayList<>(defenderPlayerState.getBoard());

        // 전투 후 하수인 제거
        attackerPlayerState.getBoard().removeIf(Minion::isDead);
        defenderPlayerState.getBoard().removeIf(Minion::isDead);
        
        // ON_MINION_DEATH 능력 발동
        // 공격자 측 죽은 하수인
        attackerMinionsBeforeRemoval.stream()
            .filter(minion -> minion.isDead() && !attackerPlayerState.getBoard().contains(minion)) // 죽었고, 보드에 없음
            .forEach(deadMinion -> executeAbilities(roomNo, attackerPlayerState.getPlayerId(), "ON_MINION_DEATH", deadMinion.getCardDTO(), null));

        // 수비자 측 죽은 하수인
        defenderMinionsBeforeRemoval.stream()
            .filter(minion -> minion.isDead() && !defenderPlayerState.getBoard().contains(minion)) // 죽었고, 보드에 없음
            .forEach(deadMinion -> executeAbilities(roomNo, defenderPlayerState.getPlayerId(), "ON_MINION_DEATH", deadMinion.getCardDTO(), null));
        
        if (defenderPlayerState.getHealth() <= 0) {
            endGame(roomNo, attackerPlayerState.getPlayerId(), defenderPlayerState.getPlayerId(), "영웅 처치");
        } else if (attackerPlayerState.getHealth() <= 0) {
            endGame(roomNo, defenderPlayerState.getPlayerId(), attackerPlayerState.getPlayerId(), "영웅 처치");
        } else {
            broadcastGameState(roomNo);
        }
    }

    public void useHeroPower(int roomNo, String playerId, String targetPlayerId, int targetIndex) {
        log.debug("[HERO POWER] Room: {}, Player: {}, TargetPlayerId: {}, TargetIndex: {}", roomNo, playerId, targetPlayerId, targetIndex);

        GameState gameState = activeGames.get(roomNo);
        if (gameState == null || !gameState.getCurrentTurnPlayerId().equals(playerId)) {
            sendErrorToPlayer(playerId, "지금은 영웅 능력을 사용할 수 없습니다. (턴 아님)");
            log.warn("[HERO POWER] Not current player's turn or game state not found. Player: {}", playerId);
            return;
        }

        PlayerState playerState = gameState.getPlayers().get(playerId);
        HeroVO hero = playerState.getHero();
        if (hero == null) {
            sendErrorToPlayer(playerId, "영웅 정보가 없어 영웅 능력을 사용할 수 없습니다.");
            log.error("[HERO POWER] HeroVO is null for player {}", playerId);
            return;
        }
        
        if (playerState.isHasUsedHeroPowerThisTurn()) {
            sendErrorToPlayer(playerId, "이번 턴에 영웅 능력을 이미 사용했습니다.");
            log.info("[HERO POWER] Hero power already used this turn by {}", playerId);
            return;
        }
        if (playerState.getMana() < hero.getHeroPowerCost()) {
            sendErrorToPlayer(playerId, "마나가 부족합니다.");
            log.info("[HERO POWER] Not enough mana for hero power. Player: {}, Mana: {}, Cost: {}", playerId, playerState.getMana(), hero.getHeroPowerCost());
            return;
        }

        playerState.setMana(playerState.getMana() - hero.getHeroPowerCost());
        playerState.setHasUsedHeroPowerThisTurn(true);
        log.info("[HERO POWER] {} used hero power. Mana remaining: {}", playerId, playerState.getMana());

        // 영웅 능력 클래스별 효과 구현
        switch (hero.getClassName()) {
            case "마법사":
                log.debug("[HERO POWER] Mage hero power activated.");
                
                PlayerState targetPlayerStateMage = gameState.getPlayers().get(targetPlayerId);
                Minion targetMinionMage = null;

                if (targetPlayerStateMage == null) {
                    sendErrorToPlayer(playerId, "유효하지 않은 마법사 영웅 능력 대상 플레이어입니다.");
                    log.warn("[HERO POWER] Invalid target player for Mage hero power: {}", targetPlayerId);
                    return;
                }

                if (targetIndex >= 0) { 
                    if (targetIndex < targetPlayerStateMage.getBoard().size()) {
                        targetMinionMage = targetPlayerStateMage.getBoard().get(targetIndex);
                    }
                    if (targetMinionMage == null) {
                        sendErrorToPlayer(playerId, "유효하지 않은 마법사 영웅 능력 대상 하수인입니다.");
                        log.warn("[HERO POWER] Invalid target minion for Mage hero power: {}", targetIndex);
                        return;
                    }
                } else if (targetIndex != -1) { 
                     sendErrorToPlayer(playerId, "유효하지 않은 마법사 영웅 능력 대상 인덱스입니다.");
                     log.warn("[HERO POWER] Invalid targetIndex (neither hero nor minion) for Mage hero power: {}", targetIndex);
                     return;
                }

                if (targetMinionMage != null) { 
                    log.debug("[HERO POWER] Mage targeting minion {}. Damage: 1", targetMinionMage.getCardDTO().getName()); 
                    targetMinionMage.takeDamage(1);
                    targetPlayerStateMage.getBoard().removeIf(Minion::isDead);
                } else { 
                    log.debug("[HERO POWER] Mage targeting hero {}. Damage: 1", targetPlayerStateMage.getPlayerId());
                    targetPlayerStateMage.setHealth(targetPlayerStateMage.getHealth() - 1);
                }
                break;

            case "흑마법사":
                log.debug("[HERO POWER] Warlock hero power (Life Tap) activated.");
                playerState.setHealth(playerState.getHealth() - 2); 
                drawCards(playerState, 1); 
                log.debug("[HERO POWER] Warlock takes 2 damage, draws 1 card. New health: {}", playerState.getHealth());
                break;

            case "사냥꾼":
                log.debug("[HERO POWER] Hunter hero power (Steady Shot) activated.");
                PlayerState opponentStateHunter = gameState.getPlayers().get(targetPlayerId);
                
                if (opponentStateHunter == null) {
                    sendErrorToPlayer(playerId, "상대 플레이어 정보를 찾을 수 없습니다.");
                    log.error("[HERO POWER] Opponent state not found for Hunter hero power in room {}", roomNo);
                    return;
                }
                opponentStateHunter.setHealth(opponentStateHunter.getHealth() - 2); 
                log.debug("[HERO POWER] Hunter deals 2 damage to enemy hero. Enemy health: {}", opponentStateHunter.getHealth());
                break;

            case "사제":
                log.debug("[HERO POWER] Priest hero power (Lesser Heal) activated.");
                
                PlayerState targetPlayerStatePriest = gameState.getPlayers().get(targetPlayerId);
                Minion targetMinionPriest = null;

                if (targetPlayerStatePriest == null) {
                    sendErrorToPlayer(playerId, "유효하지 않은 사제 영웅 능력 대상 플레이어입니다.");
                    log.warn("[HERO POWER] Invalid target player for Priest hero power: {}", targetPlayerId);
                    return;
                }

                if (targetIndex >= 0) { 
                    if (targetIndex < targetPlayerStatePriest.getBoard().size()) {
                        targetMinionPriest = targetPlayerStatePriest.getBoard().get(targetIndex);
                    }
                    if (targetMinionPriest == null) {
                        sendErrorToPlayer(playerId, "유효하지 않은 사제 영웅 능력 대상 하수인입니다.");
                        log.warn("[HERO POWER] Invalid target minion for Priest hero power: {}", targetIndex);
                        return;
                    }
                } else if (targetIndex != -1) { 
                    sendErrorToPlayer(playerId, "유효하지 않은 사제 영웅 능력 대상 인덱스입니다.");
                    log.warn("[HERO POWER] Invalid targetIndex (neither hero nor minion) for Priest hero power: {}", targetIndex);
                    return;
                }

                if (targetMinionPriest != null) { 
                    int maxMinionHealth = targetMinionPriest.getCardDTO().getHealth(); 
                    targetMinionPriest.setCurrentHealth(Math.min(targetMinionPriest.getCurrentHealth() + 2, maxMinionHealth));
                    log.debug("[HERO POWER] Priest heals minion {}. New health: {}", targetMinionPriest.getCardDTO().getName(), targetMinionPriest.getCurrentHealth());
                } else { 
                    int maxHeroHealth = 30; 
                    targetPlayerStatePriest.setHealth(Math.min(targetPlayerStatePriest.getHealth() + 2, maxHeroHealth));
                    log.debug("[HERO POWER] Priest heals hero {}. New health: {}", targetPlayerStatePriest.getPlayerId(), targetPlayerStatePriest.getHealth());
                }
                break;

            default:
                sendErrorToPlayer(playerId, hero.getClassName() + " 영웅 능력은 아직 구현되지 않았습니다.");
                log.info("[HERO POWER] Hero power for {} is not yet implemented.", hero.getClassName());
                return;
        }
        
        if (gameState.getPlayers().values().stream().anyMatch(p -> p.getHealth() <= 0)) {
            String winnerId = gameState.getPlayers().values().stream().filter(p -> p.getHealth() > 0).findFirst().orElse(null).getPlayerId();
            String loserId = gameState.getPlayers().values().stream().filter(p -> p.getHealth() <= 0).findFirst().orElse(null).getPlayerId();
            endGame(roomNo, winnerId, loserId, "영웅 처치");
        } else {
            broadcastGameState(roomNo);
        }
    }

    @Transactional
    public void concedeGame(int roomNo, String loserId) {
        GameState gameState = activeGames.get(roomNo);
        String winnerId = null;
        if (gameState == null) {
            Optional<GameRoomEntity> roomOpt = roomRepository.findById(roomNo);
            if (roomOpt.isPresent()) {
                GameRoomEntity room = roomOpt.get();
                winnerId = loserId.equals(room.getPlayer1()) ? room.getPlayer2() : room.getPlayer1();
            }
            endGame(roomNo, winnerId, loserId, "항복 (메모리 누락)");
            return;
        }
        winnerId = gameState.getPlayers().keySet().stream().filter(pid -> !pid.equals(loserId)).findFirst().orElse(null);
        endGame(roomNo, winnerId, loserId, "항복");
    }

    @Transactional
    public void endGame(int roomNo, String winnerId, String loserId, String reason) {
        roomRepository.findById(roomNo).ifPresent(room -> {
            if (!"end".equals(room.getRoomStatus())) {
                room.setRoomStatus("end");
                room.setEndAt(LocalDateTime.now());
                roomRepository.save(room);
            }
        });
        updateGameResult(winnerId, loserId);
        messagingTemplate.convertAndSend("/topic/game/" + roomNo, Map.of("type", "GAME_OVER", "winner", winnerId, "loser", loserId, "reason", reason));
        activeGames.remove(roomNo);
    }
    
    private void drawCards(PlayerState player, int count) {
        for (int i = 0; i < count; i++) {
            if (player.getDeck().isEmpty()) { break; }
            CardDTO card = player.getDeck().remove(0); 
            player.getHand().add(card);
        }
    }

    public GameState getGameState(int roomNo) {
        return activeGames.get(roomNo);
    }

    public boolean isGameActive(int roomNo) {
        return activeGames.containsKey(roomNo);
    }
    
    public void broadcastGameState(int roomNo) {
        if (activeGames.containsKey(roomNo)) {
            messagingTemplate.convertAndSend("/topic/game/" + roomNo + "/state", new GameStateDTO(activeGames.get(roomNo)));
        }
    }
    
    public void sendGameStateToPlayer(int roomNo, String playerId) {
        if (activeGames.containsKey(roomNo)) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/gamestate", new GameStateDTO(activeGames.get(roomNo)));
        }
    }
    
    private PlayerState getOpponentPlayerState(GameState gameState, String currentId) {
        return gameState.getPlayers().values().stream()
                .filter(p -> !p.getPlayerId().equals(currentId)).findFirst().orElse(null);
    }

    private void sendErrorToPlayer(String playerId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(playerId, "/queue/errors", errorMessage);
    }

    @Transactional
    protected void updateGameResult(String winnerId, String loserId) {
        if (winnerId != null) { userRepository.findById(winnerId).ifPresent(user -> { user.setWinCnt(user.getWinCnt() + 1); userRepository.save(user); }); }
        if (loserId != null) { userRepository.findById(loserId).ifPresent(user -> { user.setLoseCnt(user.getLoseCnt() + 1); userRepository.save(user); }); }
    }

    // --- 새로운 능력 실행 관련 헬퍼 메소드 추가 시작 ---

    /**
     * 특정 트리거 이벤트에 반응하는 모든 카드 능력을 찾아서 실행합니다.
     * @param roomNo 게임방 번호
     * @param casterPlayerId 능력을 발동시킨 플레이어의 ID
     * @param triggerEvent 발동 조건 (예: "ON_PLAY", "ON_TURN_START")
     * @param triggeringCard 능력을 발동시킨 카드 (선택 사항, 예: ON_PLAY 능력의 카드)
     * @param primaryTargetCard 능력이 직접적으로 적용될 대상 카드/하수인 (선택 사항)
     */
    private void executeAbilities(int roomNo, String casterPlayerId, String triggerEvent, CardDTO triggeringCard, CardDTO primaryTargetCard) {
        GameState gameState = activeGames.get(roomNo);
        if (gameState == null) {
            log.warn("[EXEC ABILITIES] GameState not found for room {}", roomNo);
            return;
        }

        PlayerState casterState = gameState.getPlayers().get(casterPlayerId);
        PlayerState opponentState = getOpponentPlayerState(gameState, casterPlayerId);
        
        if (casterState == null) {
            log.error("[EXEC ABILITIES] Caster state not found for ID: {}", casterPlayerId);
            return;
        }

        // 1. 현재 플레이어의 필드 하수인 능력
        casterState.getBoard().forEach(minion -> {
            if (minion.getCardDTO().getAbilities() != null) {
                minion.getCardDTO().getAbilities().stream()
                    .filter(ability -> ability.getTriggerEvent().equals(triggerEvent))
                    .forEach(ability -> executeAbility(roomNo, casterPlayerId, ability, triggeringCard, primaryTargetCard));
            }
        });

        // 2. 상대방의 필드 하수인 능력
        if (opponentState != null) {
            opponentState.getBoard().forEach(minion -> {
                 if (minion.getCardDTO().getAbilities() != null) {
                    minion.getCardDTO().getAbilities().stream()
                        .filter(ability -> ability.getTriggerEvent().equals(triggerEvent))
                        .forEach(ability -> executeAbility(roomNo, opponentState.getPlayerId(), ability, triggeringCard, primaryTargetCard));
                 }
            });
        }
    }

    /**
     * 단일 카드 능력을 실행합니다.
     * @param roomNo 게임방 번호
     * @param abilityCasterPlayerId 능력을 발동시킨 플레이어 ID (능력을 가진 하수인의 주인)
     * @param ability 실행할 카드 능력 엔티티
     * @param triggeringCard 능력을 발동시킨 카드 (예: ON_PLAY 능력의 카드)
     * @param primaryTargetCard 능력이 직접적으로 적용될 주된 대상 (예: 공격당한 하수인, 주문의 직접 대상)
     */
    private void executeAbility(int roomNo, String abilityCasterPlayerId, CardAbilityEntity ability, CardDTO triggeringCard, CardDTO primaryTargetCard) {
        log.debug("[ABILITY EXEC] Room: {}, Ability Caster: {}, Ability: {}, Triggering Card: {}, Primary Target Card: {}", 
                  roomNo, abilityCasterPlayerId, ability.getEffectType(), 
                  (triggeringCard != null ? triggeringCard.getName() : "N/A"),
                  (primaryTargetCard != null ? primaryTargetCard.getName() : "N/A"));

        GameState gameState = activeGames.get(roomNo);
        if (gameState == null) {
            log.warn("[ABILITY EXEC] GameState not found for room {} during ability execution.", roomNo);
            return;
        }

        PlayerState abilityCasterState = gameState.getPlayers().get(abilityCasterPlayerId);
        PlayerState opponentState = getOpponentPlayerState(gameState, abilityCasterPlayerId);

        if (abilityCasterState == null) {
            log.error("[ABILITY EXEC] Ability caster state not found for ID: {}", abilityCasterPlayerId);
            return;
        }

        Map<String, Object> params = ability.getEffectParamsJson() != null ? ability.getEffectParamsJson() : Collections.emptyMap(); // 수정된 부분

        List<Object> targets = getTargetsForAbility(gameState, abilityCasterState, opponentState, ability.getTargetScope(), primaryTargetCard);

        for (Object target : targets) {
            if (target == null) {
                log.warn("[ABILITY EXEC] Resolved target is null for scope: {}", ability.getTargetScope());
                continue;
            }
            switch (ability.getEffectType()) {
                case "DEAL_DAMAGE":
                    handleDamageEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "HEAL":
                    handleHealEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "DRAW_CARD":
                    handleDrawCardEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "SUMMON_MINION":
                    handleSummonMinionEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "GAIN_MANA":
                    handleGainManaEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "GAIN_ARMOR":
                    handleGainArmorEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                case "MODIFY_STATS":
                    handleModifyStatsEffect(roomNo, abilityCasterState, opponentState, target, params);
                    break;
                default:
                    log.warn("[ABILITY EXEC] Unknown effect type: {}", ability.getEffectType());
                    break;
            }
        }
        
        abilityCasterState.getBoard().removeIf(Minion::isDead);
        if(opponentState != null) {
            opponentState.getBoard().removeIf(Minion::isDead);
        }
    }

    private List<Object> getTargetsForAbility(GameState gameState, PlayerState casterState, PlayerState opponentState, String targetScope, CardDTO primaryTargetCard) {
        List<Object> targets = new ArrayList<>();
        switch (targetScope) {
            case "SELF": 
                targets.add(casterState);
                break;
            case "ENEMY_HERO": 
                if (opponentState != null) targets.add(opponentState);
                break;
            case "FRIENDLY_MINION": 
                targets.addAll(casterState.getBoard());
                break;
            case "ENEMY_MINION": 
                if (opponentState != null) targets.addAll(opponentState.getBoard());
                break;
            case "SINGLE_MINION": 
                if (primaryTargetCard != null) { 
                    casterState.getBoard().stream()
                        .filter(m -> m.getCardDTO().getNo() == primaryTargetCard.getNo())
                        .findFirst().ifPresent(targets::add);
                    if (targets.isEmpty() && opponentState != null) { 
                        opponentState.getBoard().stream()
                            .filter(m -> m.getCardDTO().getNo() == primaryTargetCard.getNo())
                            .findFirst().ifPresent(targets::add);
                    }
                } else {
                    log.warn("[ABILITY TARGET] SINGLE_MINION targetScope without explicit primaryTargetCard. Not supported for dynamic targeting.");
                }
                break;
            case "SINGLE_CHARACTER": 
                 if (primaryTargetCard != null) { 
                    log.warn("[ABILITY TARGET] SINGLE_CHARACTER targetScope with primaryTargetCard (CardDTO) as Hero is not yet fully supported.");
                    // 임시 로직: CardDTO의 No가 플레이어 영웅 ID와 일치하면 해당 영웅을 대상으로
                    // 이는 임시 방편이며, 영웅을 CardDTO로 표현하는 방식에 따라 수정이 필요함.
                    if (casterState.getHero() != null && primaryTargetCard.getNo() == casterState.getHero().getNo()) { // No는 int
                         targets.add(casterState);
                    } else if (opponentState != null && opponentState.getHero() != null && primaryTargetCard.getNo() == opponentState.getHero().getNo()) {
                         targets.add(opponentState);
                    } else { // 영웅이 아니면 하수인을 찾음
                        casterState.getBoard().stream()
                            .filter(m -> m.getCardDTO().getNo() == primaryTargetCard.getNo())
                            .findFirst().ifPresent(targets::add);
                        if (targets.isEmpty() && opponentState != null) { 
                            opponentState.getBoard().stream()
                                .filter(m -> m.getCardDTO().getNo() == primaryTargetCard.getNo())
                                .findFirst().ifPresent(targets::add);
                        }
                    }
                } else {
                    log.warn("[ABILITY TARGET] SINGLE_CHARACTER targetScope without explicit primaryTargetCard. Not supported for dynamic targeting.");
                }
                break;
            case "ALL_ENEMIES": 
                if (opponentState != null) {
                    targets.add(opponentState);
                    targets.addAll(opponentState.getBoard());
                }
                break;
            case "RANDOM_ENEMY_MINION": 
                if (opponentState != null && !opponentState.getBoard().isEmpty()) {
                    targets.add(opponentState.getBoard().get(new Random().nextInt(opponentState.getBoard().size())));
                }
                break;
            case "SELF_BOARD": 
                targets.addAll(casterState.getBoard());
                break;
            case "ENEMY_BOARD": 
                if (opponentState != null) targets.addAll(opponentState.getBoard());
                break;
            case "ALL_CHARACTERS": 
                targets.add(casterState);
                if (opponentState != null) targets.add(opponentState);
                targets.addAll(casterState.getBoard());
                if (opponentState != null) targets.addAll(opponentState.getBoard());
                break;
            case "ALL_HEROES": 
                targets.add(casterState);
                if (opponentState != null) targets.add(opponentState);
                break;
            case "NONE": 
                break;
            default:
                log.warn("[ABILITY TARGET] Unknown target scope: {}", targetScope);
                break;
        }
        return targets;
    }

    private void handleDamageEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int amount = (Integer) params.getOrDefault("amount", 0);
        if (amount == 0) return;

        if (target instanceof PlayerState) {
            PlayerState targetPlayer = (PlayerState) target;
            targetPlayer.setHealth(targetPlayer.getHealth() - amount);
            log.debug("[ABILITY DMG] Player {} takes {} damage. New health: {}", targetPlayer.getPlayerId(), amount, targetPlayer.getHealth());
        } else if (target instanceof Minion) {
            Minion targetMinion = (Minion) target;
            targetMinion.takeDamage(amount);
            log.debug("[ABILITY DMG] Minion {} takes {} damage. New health: {}", targetMinion.getCardDTO().getName(), amount, targetMinion.getCurrentHealth());
        }
    }

    private void handleHealEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int amount = (Integer) params.getOrDefault("amount", 0);
        if (amount == 0) return;

        if (target instanceof PlayerState) {
            PlayerState targetPlayer = (PlayerState) target;
            int maxHealth = 30; 
            targetPlayer.setHealth(Math.min(targetPlayer.getHealth() + amount, maxHealth));
            log.debug("[ABILITY HEAL] Player {} heals {} health. New health: {}", targetPlayer.getPlayerId(), amount, targetPlayer.getHealth());
        } else if (target instanceof Minion) {
            Minion targetMinion = (Minion) target;
            int maxHealth = targetMinion.getCardDTO().getHealth(); 
            targetMinion.setCurrentHealth(Math.min(targetMinion.getCurrentHealth() + amount, maxHealth));
            log.debug("[ABILITY HEAL] Minion {} heals {} health. New health: {}", targetMinion.getCardDTO().getName(), amount, targetMinion.getCurrentHealth());
        }
    }

    private void handleDrawCardEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int amount = (Integer) params.getOrDefault("amount", 1); 
        if (target instanceof PlayerState) {
            PlayerState targetPlayer = (PlayerState) target;
            drawCards(targetPlayer, amount);
            log.debug("[ABILITY DRAW] Player {} draws {} cards.", targetPlayer.getPlayerId(), amount);
        } else {
            log.warn("[ABILITY DRAW] Invalid target for DRAW_CARD effect: {}", target.getClass().getSimpleName());
        }
    }

    private void handleSummonMinionEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int attack = (Integer) params.getOrDefault("attack", 1);
        int health = (Integer) params.getOrDefault("health", 1);
        String cardNoStr = (String) params.get("card_no"); 
        
        CardDTO summonedCardDTO = null;
        if (cardNoStr != null) { 
            try {
                int cardNo = Integer.parseInt(cardNoStr);
                CardVO cardVO = cardRepository.findById(cardNo).orElse(null); 
                if (cardVO != null) {
                     List<CardAbilityEntity> abilities = cardAbilityRepository.findByCardNo(cardVO.getNo());
                     summonedCardDTO = new CardDTO(cardVO, abilities);
                }
            } catch (NumberFormatException e) {
                log.error("[ABILITY SUMMON] Invalid card_no format: {}", cardNoStr, e);
            }
        } else { 
            CardVO tempCardVO = new CardVO();
            tempCardVO.setNo(-1); 
            tempCardVO.setName("소환된 하수인");
            tempCardVO.setAttack(attack);
            tempCardVO.setHealth(health);
            summonedCardDTO = new CardDTO(tempCardVO, Collections.emptyList());
        }

        if (summonedCardDTO != null) {
            if (target instanceof PlayerState) { 
                PlayerState targetPlayer = (PlayerState) target;
                if (targetPlayer.getBoard().size() < 7) { 
                    targetPlayer.getBoard().add(new Minion(summonedCardDTO));
                    log.debug("[ABILITY SUMMON] Player {} summons a minion ({}). Board size: {}", targetPlayer.getPlayerId(), summonedCardDTO.getName(), targetPlayer.getBoard().size());
                } else {
                    log.info("[ABILITY SUMMON] Player {} board is full, cannot summon minion.", targetPlayer.getPlayerId());
                }
            } else {
                log.warn("[ABILITY SUMMON] Invalid target for SUMMON_MINION effect: {}", target.getClass().getSimpleName());
            }
        }
    }

    private void handleGainManaEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int amount = (Integer) params.getOrDefault("amount", 1);
        if (amount == 0) return;

        if (target instanceof PlayerState) {
            PlayerState targetPlayer = (PlayerState) target;
            targetPlayer.setMana(targetPlayer.getMana() + amount);
            targetPlayer.setMaxMana(Math.min(targetPlayer.getMaxMana() + amount, 10)); 
            log.debug("[ABILITY MANA] Player {} gains {} mana. New mana: {}", targetPlayer.getPlayerId(), amount, targetPlayer.getMana());
        } else {
            log.warn("[ABILITY MANA] Invalid target for GAIN_MANA effect: {}", target.getClass().getSimpleName());
        }
    }

    private void handleGainArmorEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int amount = (Integer) params.getOrDefault("amount", 0);
        if (amount == 0) return;

        if (target instanceof PlayerState) {
            PlayerState targetPlayer = (PlayerState) target;
            // TODO: 방어도 필드 추가 및 업데이트 로직 구현
            log.warn("[ABILITY ARMOR] Gain Armor effect is not fully implemented yet (no armor field in PlayerState). Player {} gains {} armor.", targetPlayer.getPlayerId(), amount);
        } else {
            log.warn("[ABILITY ARMOR] Invalid target for GAIN_ARMOR effect: {}", target.getClass().getSimpleName());
        }
    }
    
    private void handleModifyStatsEffect(int roomNo, PlayerState casterState, PlayerState opponentState, Object target, Map<String, Object> params) {
        int attackGain = (Integer) params.getOrDefault("attack_gain", 0);
        int healthGain = (Integer) params.getOrDefault("health_gain", 0);
        String duration = (String) params.getOrDefault("duration", "PERMANENT"); 

        if (target instanceof Minion) {
            Minion targetMinion = (Minion) target;
            targetMinion.getCardDTO().setAttack(targetMinion.getCardDTO().getAttack() + attackGain); 
            targetMinion.getCardDTO().setHealth(targetMinion.getCardDTO().getHealth() + healthGain); 
            targetMinion.setCurrentHealth(targetMinion.getCurrentHealth() + healthGain); 

            log.debug("[ABILITY STATS] Minion {} gets +{}/+{} stats. Current health: {}", targetMinion.getCardDTO().getName(), attackGain, healthGain, targetMinion.getCurrentHealth());
            
            // TODO: 'THIS_TURN'과 같은 지속 시간 관리를 위한 추가 로직 필요 (턴 종료 시 원래대로 되돌리는 등)
        } else {
            log.warn("[ABILITY STATS] Invalid target for MODIFY_STATS effect: {}", target.getClass().getSimpleName());
        }
    }
    
    public void handleCardHover(int roomNo, String playerId, int cardIndex) {
        GameState gameState = activeGames.get(roomNo);
        if (gameState == null) {
            return; // 게임이 없으면 아무것도 하지 않음
        }

        // 상대방 플레이어 ID 찾기
        String opponentId = gameState.getPlayers().keySet().stream()
                .filter(pid -> !pid.equals(playerId))
                .findFirst()
                .orElse(null);

        if (opponentId != null) {
            // 상대방에게만 'OPPONENT_CARD_HOVER' 이벤트를 보냄
            // Map.of를 사용하여 간단하게 JSON 객체를 만듭니다.
            messagingTemplate.convertAndSendToUser(
                opponentId, 
                "/queue/event", 
                Map.of("type", "OPPONENT_CARD_HOVER", "cardIndex", cardIndex)
            );
        }
    }

}