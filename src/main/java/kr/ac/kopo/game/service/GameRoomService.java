package kr.ac.kopo.game.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.kopo.game.repository.GameRoomRepository;
import kr.ac.kopo.game.vo.GameRoomEntity;
import kr.ac.kopo.game.vo.RoomStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GameRoomService {

    @Autowired private GameRoomRepository roomRepository;
    @Autowired private GameService gameService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private static final List<String> ACTIVE_STATUSES = List.of(RoomStatus.WAITING.name(), RoomStatus.GAMING.name());

    @Transactional
    public GameRoomEntity findAndJoinRoom(String playerId, int deckId) {
        
        // 1. 내가 이미 참여 중인 활성 상태의 방이 있는지 확인 (기존 로직 유지)
        Optional<GameRoomEntity> myExistingRoom = findAndCleanMyActiveRoom(playerId);
        if(myExistingRoom.isPresent()) {
            log.info("### 플레이어 [{}]가 활성 상태의 방({})에 재입장합니다.", playerId, myExistingRoom.get().getNo());
            return myExistingRoom.get();
        }
        
        // ✨ [새로운 로직]
        // 2. 참여할 대기방을 찾기 전, 너무 오래된 대기방(Stale Room)을 먼저 정리합니다.
        List<GameRoomEntity> allWaitingRooms = roomRepository.findAllByRoomStatus("WAITING");
        allWaitingRooms.forEach(room -> {
            // 만들어진 지 2분 이상 지난 대기방은 버려진 것으로 간주하고 정리
            if (room.getStartAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
                log.warn("### 2분 이상 방치된 대기방({})을 정리합니다.", room.getNo());
                room.setRoomStatus(RoomStatus.END.name());
                room.setEndAt(LocalDateTime.now());
                // roomRepository.save(room); // 아래 saveAll로 한번에 처리
            }
        });
        roomRepository.saveAll(allWaitingRooms); // 변경된 상태 일괄 저장

        // 3. 정리된 후 남아있는 대기방을 찾아 참가 시도
        Optional<GameRoomEntity> waitingRoomToJoin = roomRepository.findFirstWaitingForUpdate().stream().findFirst();

        if (waitingRoomToJoin.isPresent()) {
            GameRoomEntity roomToJoin = waitingRoomToJoin.get();
            log.info("### 플레이어 [{}]가 대기 중인 방({})에 참가합니다.", playerId, roomToJoin.getNo());
            roomToJoin.setPlayer2(playerId);
            roomToJoin.setPlayer2DeckId(deckId);
            roomToJoin.setRoomStatus(RoomStatus.GAMING.name());
            GameRoomEntity savedRoom = roomRepository.save(roomToJoin);
            
            gameService.initializeGame(savedRoom);
            
            messagingTemplate.convertAndSend("/topic/game/" + savedRoom.getNo(), 
                java.util.Map.of("type", "REDIRECT", "url", "/game/room?roomNo=" + savedRoom.getNo()));

            return savedRoom;
        } else {
            log.info("### 플레이어 [{}]가 새로운 대기방을 생성합니다.", playerId);
            GameRoomEntity newRoom = new GameRoomEntity();
            newRoom.setPlayer1(playerId);
            newRoom.setPlayer1DeckId(deckId);
            newRoom.setRoomStatus(RoomStatus.WAITING.name());
            newRoom.setStartAt(LocalDateTime.now());
            return roomRepository.save(newRoom);
        }
    }

    private Optional<GameRoomEntity> findAndCleanMyActiveRoom(String playerId) {
        List<GameRoomEntity> myActiveRooms = roomRepository.findActiveRoomByPlayerId(playerId, ACTIVE_STATUSES);
        myActiveRooms.forEach(room -> {
            if ("gaming".equalsIgnoreCase(room.getRoomStatus()) && !gameService.isGameActive(room.getNo())) {
                log.info("### 유효하지 않은 게임 방(Room {})을 발견했습니다. 정리합니다.", room.getNo());
                room.setRoomStatus(RoomStatus.END.name());
                room.setEndAt(LocalDateTime.now());
                roomRepository.save(room);
            }
        });
        return roomRepository.findActiveRoomByPlayerId(playerId, ACTIVE_STATUSES).stream().findFirst();
    }

    /**
     * 특정 방 번호의 GameRoomEntity 정보를 가져옵니다.
     * @param roomNo 방 번호
     * @return GameRoomEntity 객체 (존재하지 않으면 null)
     */
    public GameRoomEntity getRoomInfo(int roomNo) {
        return roomRepository.findById(roomNo).orElse(null);
    }

    /**
     * 특정 방이 WAITING 상태인지 확인합니다.
     * @param roomNo 방 번호
     * @return WAITING 상태이면 true, 아니면 false
     */
    public boolean isWaitingRoom(Integer roomNo) {
        Optional<GameRoomEntity> room = roomRepository.findById(roomNo);
        return room.isPresent() && RoomStatus.WAITING.name().equalsIgnoreCase(room.get().getRoomStatus());
    }

    /**
     * 웹소켓 연결이 끊어졌을 때 대기 중인 방을 정리합니다.
     * 방이 WAITING 상태이고 아직 플레이어2가 없는 경우에만 방을 종료 상태로 변경합니다.
     * @param roomNo 정리할 방 번호
     */
    @Transactional
    public void cleanupWaitingRoom(Integer roomNo) {
        roomRepository.findById(roomNo).ifPresent(room -> {
            // 방이 WAITING 상태이고, 아직 게임이 시작되지 않았다면 (즉, player2가 아직 없다면)
            // 해당 방을 종료 상태로 변경합니다.
            if (RoomStatus.WAITING.name().equalsIgnoreCase(room.getRoomStatus()) && room.getPlayer2() == null) {
                log.info("### 대기방({})을 정리합니다. 플레이어 이탈.", room.getNo());
                room.setRoomStatus(RoomStatus.END.name());
                room.setEndAt(LocalDateTime.now());
                roomRepository.save(room);
            } else if (RoomStatus.GAMING.name().equalsIgnoreCase(room.getRoomStatus()) && !gameService.isGameActive(room.getNo())) {
                // 이미 게임 중으로 전환되었으나 gameService에서 활성화되지 않은 경우, 정리 (중복 체크 방지)
                log.info("### 이미 게임 시작된 것으로 보이나 활성화되지 않은 게임 방({})을 정리합니다.", room.getNo());
                room.setRoomStatus(RoomStatus.END.name());
                room.setEndAt(LocalDateTime.now());
                roomRepository.save(room);
            }
        });
    }
}