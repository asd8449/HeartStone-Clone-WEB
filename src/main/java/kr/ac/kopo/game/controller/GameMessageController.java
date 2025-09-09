package kr.ac.kopo.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.game.service.GameService;
import kr.ac.kopo.game.vo.PlayerAction;
import kr.ac.kopo.user.vo.UserEntity;

@Controller
public class GameMessageController {

    @Autowired
    private GameService gameService;

    @MessageMapping("/game/{roomNo}/action")
    public void handleAction(@DestinationVariable("roomNo") int roomNo, @Payload PlayerAction action) {
        switch (action.getType()) {
            case CONCEDE:
                gameService.concedeGame(roomNo, action.getPlayerId());
                break;
            case END_TURN:
                gameService.endTurn(roomNo, action.getPlayerId());
                break;
            case PLAY_CARD:
                gameService.playCard(roomNo, action.getPlayerId(), action.getCardId(), action.getInsertIndex());
                break;
            case ATTACK:
                gameService.attack(roomNo, action.getPlayerId(), action.getAttackerIndex(), action.getTargetIndex());
                break;
            case USE_HERO_POWER:
                gameService.useHeroPower(roomNo, action.getPlayerId(), action.getTargetPlayerId(), action.getTargetIndex());
                break;
            // ✨ 아래 case를 새로 추가해주세요.
            case HOVER_CARD:
                gameService.handleCardHover(roomNo, action.getPlayerId(), action.getInsertIndex());
                break;
            default:
                break;
        }
    }
    
    @MessageMapping("/game/{roomNo}/requestState")
    public void requestGameState(@DestinationVariable("roomNo") int roomNo, SimpMessageHeaderAccessor headerAccessor) {
        HttpSession httpSession = (HttpSession) headerAccessor.getSessionAttributes().get("httpSession");
        if (httpSession != null) {
            UserEntity user = (UserEntity) httpSession.getAttribute("user");
            if (user != null) {
                gameService.sendGameStateToPlayer(roomNo, user.getId());
            }
        }
    }
}