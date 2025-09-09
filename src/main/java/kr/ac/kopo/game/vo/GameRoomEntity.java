package kr.ac.kopo.game.vo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="game_room")
public class GameRoomEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="gameroom_sequence")
    @SequenceGenerator(name="gameroom_sequence", sequenceName = "seq_game_room_no", allocationSize = 1)
    private int no;
    
    @Column(name = "player1_id")
    private String player1;
    
    @Column(name = "player2_id")
    private String player2;
    
    // 이 두 필드를 추가합니다.
    @Column(name = "player1_deck_id")
    private Integer player1DeckId;
    
    @Column(name = "player2_deck_id")
    private Integer player2DeckId;
    
    @Column(name = "room_status")
    private String roomStatus;
    
    @Column(name="start_date", updatable = false)
    private LocalDateTime startAt;
    
    @Column(name="end_date")
    private LocalDateTime endAt;
}