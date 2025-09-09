package kr.ac.kopo.game.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name="game_state")
@Entity
@Data
public class RoomStatusEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="state_sequence")
	@SequenceGenerator(name="state_sequence", sequenceName = "seq_game_state_no", allocationSize = 1)
	@Column
	private int no;
	@Column(name="room_no")
	private int roomNo;
	@Column
	private int turn;
}
