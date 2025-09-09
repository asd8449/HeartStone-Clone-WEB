package kr.ac.kopo.card.vo;

import java.time.LocalDateTime;

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
@Table(name="a_card")
@Entity
@Data
public class CardVO {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="card_sequence")
	@SequenceGenerator(name="card_sequence", sequenceName = "seq_a_card_no", allocationSize = 1)
	@Column
	private int no;
	
	@Column(name="card_id", unique = true)
	private String cardId;
	
	@Column
	private String type;
	@Column
	private String name;
	@Column
	private String description;
	@Column
	private int cost;
	@Column
	private int attack;
	@Column
	private int health;
	@Column(name="reg_date", insertable = false, updatable = false)
	private LocalDateTime regDate;
	@Column
	private String keyword;
    @Column(name="image_url")
    private String imageUrl;
}
