package kr.ac.kopo.deck.vo;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kr.ac.kopo.user.vo.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name="deck")
@Entity
@Data
public class DeckEntity {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="deck_sequence")
	@SequenceGenerator(name="deck_sequence", sequenceName = "seq_deck_no", allocationSize = 1)
	@Column
	private int no;

    // user_id 컬럼을 UserEntity 객체와 매핑합니다.
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

	@Column
	private String name;
	
	@Column(name="reg_date", insertable = false, updatable = false)
	private LocalDateTime regDate;
}