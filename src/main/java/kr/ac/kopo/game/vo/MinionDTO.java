package kr.ac.kopo.game.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MinionDTO {
    
    // CardVO 대신 CardDTO를 사용하며, 필드명도 cardDTO로 변경
    private CardDTO cardDTO; 
    private int currentHealth;
    private boolean canAttack;
    private boolean hasAttackedThisTurn;
    private boolean hasTaunt; // Minion 클래스의 hasTaunt() 메소드에서 가져옴

    public MinionDTO(Minion entity) {
        // entity.getCardVO() 대신 entity.getCardDTO()를 사용
        // 또한, CardDTO 객체를 새로 생성할 필요 없이, 기존 CardDTO 객체를 직접 할당합니다.
        if (entity.getCardDTO() != null) { 
            this.cardDTO = entity.getCardDTO(); 
        }
        this.currentHealth = entity.getCurrentHealth();
        this.canAttack = entity.isCanAttack();
        this.hasAttackedThisTurn = entity.isHasAttackedThisTurn();
        // Minion의 hasTaunt() 메소드는 boolean을 반환하므로 직접 할당
        this.hasTaunt = entity.hasTaunt(); 
    }
}