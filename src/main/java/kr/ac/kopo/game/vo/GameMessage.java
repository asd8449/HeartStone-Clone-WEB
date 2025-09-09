package kr.ac.kopo.game.vo;

import lombok.Data;

@Data
public class GameMessage {
    private String player;
    private String action;
    private long timestamp;
}