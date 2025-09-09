package kr.ac.kopo.game.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import kr.ac.kopo.game.vo.GameRoomEntity;
import org.springframework.data.domain.PageRequest;


@Repository
public interface GameRoomRepository extends JpaRepository<GameRoomEntity, Integer>{
	
	List<GameRoomEntity> findAllByRoomStatus(String status);

    @Query("SELECT g FROM GameRoomEntity g WHERE (g.player1 = :playerId OR g.player2 = :playerId) AND g.roomStatus IN :statuses")
    List<GameRoomEntity> findActiveRoomByPlayerId(@Param("playerId") String playerId, @Param("statuses") List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GameRoomEntity g WHERE g.roomStatus = 'WAITING' ORDER BY g.startAt ASC")
    List<GameRoomEntity> findFirstWaiting(Pageable pageable);

    default List<GameRoomEntity> findFirstWaitingForUpdate() {
        return findFirstWaiting(PageRequest.of(0, 1));
    }
    
    @Modifying
    @Query(value = "UPDATE game_room SET player2_id = :playerId, player2_deck_id = :deckId, room_status = 'GAMING' " +
                   "WHERE no = (SELECT no FROM (SELECT no FROM game_room WHERE room_status = 'WAITING' ORDER BY start_date ASC) WHERE ROWNUM = 1) " +
                   "AND player2_id IS NULL", nativeQuery = true)
    int joinFirstWaitingRoom(@Param("playerId") String playerId, @Param("deckId") int deckId);
    
    // 이 메소드를 수정하여, 방금 참가한 방을 더 정확하게 찾도록 합니다.
    Optional<GameRoomEntity> findByPlayer2AndRoomStatusAndPlayer1IsNotNull(String player2, String roomStatus);
}