package kr.ac.kopo.game.vo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import kr.ac.kopo.card.vo.HeroDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerStateDTO {

    private String playerId;
    private int health;
    private int mana;
    private int maxMana;
    private HeroDTO hero;
    private int deckId; 
    private int heroAttack;
    private boolean hasUsedHeroPowerThisTurn;
    private boolean heroAttackedThisTurn;
    private List<CardDTO> hand;
    private List<MinionDTO> board;
    private int deckSize;

    public PlayerStateDTO(PlayerState entity) {
        this.playerId = entity.getPlayerId();
        this.health = entity.getHealth();
        this.mana = entity.getMana();
        this.maxMana = entity.getMaxMana();

        if (entity.getHero() != null) {
            this.hero = new HeroDTO(entity.getHero()); 
        }
        
        this.deckId = entity.getDeckId(); 
        this.heroAttack = entity.getHeroAttack();
        this.hasUsedHeroPowerThisTurn = entity.isHasUsedHeroPowerThisTurn();
        this.heroAttackedThisTurn = entity.isHeroAttackedThisTurn();
        
        // 수정된 부분: hand가 이미 List<CardDTO>이므로 다시 매핑할 필요 없음
        this.hand = (entity.getHand() != null) 
            ? entity.getHand() // 직접 할당
            : Collections.emptyList();
        
        this.board = (entity.getBoard() != null) 
            ? entity.getBoard().stream().map(MinionDTO::new).collect(Collectors.toList())
            : Collections.emptyList();
        
        this.deckSize = (entity.getDeck() != null) ? entity.getDeck().size() : 0;
    }
}