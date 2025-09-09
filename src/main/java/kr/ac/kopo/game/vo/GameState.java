package kr.ac.kopo.game.vo;

import java.util.Map;

import lombok.Data;

@Data
public class GameState {
    private String gameId; // 게임방 번호와 동일
    private String currentTurnPlayerId; // 현재 턴인 플레이어 ID
    private int turnCount;
    private Map<String, PlayerState> players; // 플레이어 ID를 key로 사용
}