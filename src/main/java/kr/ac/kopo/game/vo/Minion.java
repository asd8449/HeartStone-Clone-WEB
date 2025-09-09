package kr.ac.kopo.game.vo;

// import kr.ac.kopo.card.vo.CardVO; // CardDTO를 사용하므로 이 임포트는 더 이상 필요 없습니다.
import lombok.Data;
import lombok.NoArgsConstructor;

// CardAbilityEntity 및 List 임포트는 Minion에서 직접 능력을 활용할 때 추가될 예정입니다.

@Data
@NoArgsConstructor
public class Minion {

    // 원본 카드 정보 (이제 CardDTO를 가짐)
    private CardDTO cardDTO; // CardVO -> CardDTO로 변경

    // 필드 위에서 변하는 상태
    private int currentHealth;
    private boolean canAttack = false; 
    private boolean hasAttackedThisTurn = false;

    // 생성자: 카드를 필드에 낼 때 호출
    public Minion(CardDTO cardDTO) { // CardVO -> CardDTO로 변경
        this.cardDTO = cardDTO;
        this.currentHealth = cardDTO.getHealth(); // CardDTO에서 체력 가져옴
        // '돌진' 키워드가 있으면 바로 공격 가능
        if (this.cardDTO.getKeyword() != null && this.cardDTO.getKeyword().contains("돌진")) {
            this.canAttack = true;
        }
    }
    
    // 이 하수인이 도발 능력을 가졌는지 확인
    public boolean hasTaunt() {
        return this.cardDTO.getKeyword() != null && this.cardDTO.getKeyword().contains("도발"); // CardDTO에서 키워드 가져옴
    }

    // 데미지를 받는 로직
    public void takeDamage(int damage) {
        this.currentHealth -= damage;
    }

    // 이 하수인이 죽었는지 확인
    public boolean isDead() {
        return this.currentHealth <= 0;
    }
}