package kr.ac.kopo.game.vo;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeckDTO {

    private int no;
    private String userId;
    private String name;
    private String regDate;
    
    private List<CardDTO> cards;

}