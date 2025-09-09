package kr.ac.kopo.game.vo;

import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.card.vo.CardAbilityEntity; // CardAbilityEntity 임포트 추가
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List; // List 임포트 추가


@Data
@NoArgsConstructor // Lombok: 기본 생성자를 추가합니다.
public class CardDTO {
    private int no;
    private String name;
    private String description;
    private int cost;
    private int attack;
    private int health;
    private String keyword;
    private String type;
    private String cardId;
    private String imageUrl;
    
    // 카드 능력 목록 필드 추가
    private List<CardAbilityEntity> abilities; 
    
    public CardDTO(CardVO entity) {
        this.no = entity.getNo();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.cost = entity.getCost();
        this.attack = entity.getAttack();
        this.health = entity.getHealth();
        this.keyword = entity.getKeyword();
        this.cardId = entity.getCardId();
        this.imageUrl = entity.getImageUrl();
        this.type = entity.getType();
        // abilities 필드는 여기에서 직접 초기화하지 않고, DeckService에서 별도로 설정합니다.
        // this.abilities = null; // 초기값은 null 또는 빈 리스트로 설정 가능 (DeckService에서 채워질 예정)
    }

    // CardDTO 생성자를 오버로드하여 CardVO와 abilities를 함께 받을 수 있도록 추가 (DeckService에서 활용)
    public CardDTO(CardVO entity, List<CardAbilityEntity> abilities) {
        this(entity); // 기존 CardVO를 받는 생성자 호출
        this.abilities = abilities; // abilities 필드 설정
    }
}