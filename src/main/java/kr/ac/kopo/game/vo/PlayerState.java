package kr.ac.kopo.game.vo;

import java.util.ArrayList;
import java.util.List;
import kr.ac.kopo.card.vo.HeroVO;
import lombok.Data;

@Data
public class PlayerState {
    private String playerId;
    private int health = 30;
    private int mana = 0;
    private int maxMana = 0;
    private List<CardDTO> deck = new ArrayList<>(); 
    private List<CardDTO> hand = new ArrayList<>();
    private List<Minion> board = new ArrayList<>();
    private HeroVO hero;
    
    private int deckId; 
    
    private int heroAttack = 0;
    private boolean hasUsedHeroPowerThisTurn = false;
    private boolean heroAttackedThisTurn = false; 
}