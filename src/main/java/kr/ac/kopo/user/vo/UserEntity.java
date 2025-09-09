package kr.ac.kopo.user.vo;

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
@Table(name="h_user")
@Entity
@Data
public class UserEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="user_sequence")
	@SequenceGenerator(name="user_sequence", sequenceName = "seq_h_user_no", allocationSize = 1)
	@Column
	private int no;
	@Column
	private String id;
	@Column
	private String password;
	@Column
	private String name;
	@Column(name="win_count")
	private int winCnt;
	@Column(name="lose_count")
	private int loseCnt;
	@Column(name="reg_date", insertable = false)
	private LocalDateTime regDate;
	
}
