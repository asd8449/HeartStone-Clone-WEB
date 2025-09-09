package kr.ac.kopo.game.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PlayerAction {
    private String playerId;
    private ActionType type;
    
    // 필드 추가: 카드나 하수인의 위치를 지정할 때 사용
    private int cardId;
    private int attackerIndex;
    private int targetIndex;
    private int insertIndex; // 카드를 필드에 놓을 위치 (0, 1, 2...)

    // 영웅 능력 대상 플레이어 ID 필드 추가
    private String targetPlayerId; 

    public enum ActionType {
        CONCEDE,
        END_TURN,
        PLAY_CARD,
        ATTACK,
        USE_HERO_POWER,
        HOVER_CARD
    }
}