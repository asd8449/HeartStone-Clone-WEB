package kr.ac.kopo.game.vo;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class GameStateDTO {
    private String gameId;
    private String currentTurnPlayerId;
    private int turnCount;
    private Map<String, PlayerStateDTO> players;

    public GameStateDTO(GameState entity) {
        this.gameId = entity.getGameId();
        this.currentTurnPlayerId = entity.getCurrentTurnPlayerId();
        this.turnCount = entity.getTurnCount();
        this.players = entity.getPlayers().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new PlayerStateDTO(e.getValue())));
    }
}