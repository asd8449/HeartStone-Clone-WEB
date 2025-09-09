package kr.ac.kopo.game.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.deck.service.DeckService;
import kr.ac.kopo.deck.vo.DeckEntity;
import kr.ac.kopo.game.service.GameRoomService;
import kr.ac.kopo.game.service.GameService;
import kr.ac.kopo.game.vo.GameRoomEntity;
import kr.ac.kopo.game.vo.GameState;
import kr.ac.kopo.game.vo.GameStateDTO;
import kr.ac.kopo.user.vo.UserEntity;

@Controller
public class GameController {
    
    @Autowired private GameService gameService;
    @Autowired private GameRoomService gameRoomService;
    @Autowired private DeckService deckService;

    @GetMapping("/game/select-deck")
    public ModelAndView selectDeck(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }
        
        List<DeckEntity> decks = deckService.getDecksByUserId(user.getId());
        ModelAndView mav = new ModelAndView("classic/selectDeck");
        mav.addObject("decks", decks);
        return mav;
    }

    /**
     * 이 메소드의 이름과 로직을 변경합니다.
     * 이제 이 주소는 '매칭 시작'과 '게임방 입장' 두 가지 역할을 모두 수행합니다.
     */
    @GetMapping("/game/room")
    public ModelAndView enterRoom(@RequestParam("roomNo") int roomNo, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }
        
        GameRoomEntity room = gameRoomService.getRoomInfo(roomNo);
        if (room == null) {
            // 존재하지 않는 방에 접근 시, 덱 선택 화면으로 보냅니다.
            return new ModelAndView("redirect:/classic/select-deck");
        }
        
        if ("waiting".equalsIgnoreCase(room.getRoomStatus())) {
            ModelAndView mav = new ModelAndView("classic/waiting");
            mav.addObject("roomNo", room.getNo());
            return mav;
        } else { // "gaming" 상태일 경우
            ModelAndView mav = new ModelAndView("classic/room");
            mav.addObject("roomNo", room.getNo());
            mav.addObject("user", user);

            GameState initialGameState = gameService.getGameState(room.getNo());
            if (initialGameState != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                    String gameStateJson = mapper.writeValueAsString(new GameStateDTO(initialGameState));
                    mav.addObject("initialGameStateJson", gameStateJson);
                } catch (Exception e) {
                    mav.addObject("initialGameStateJson", "null");
                }
            } else {
                mav.addObject("initialGameStateJson", "null");
            }
            return mav;
        }
    }
    
    /**
     * 덱 ID를 기반으로 게임 매칭을 시작하거나 기존 방에 참여합니다.
     * 게임 종료 후 '같은 덱으로 다시하기' 기능에서 호출됩니다.
     * @param deckId 선택된 덱 ID
     * @param session 현재 사용자 세션
     * @return 게임방으로의 리다이렉트
     */
    @GetMapping("/game/match")
    public ModelAndView matchGame(@RequestParam("deckId") int deckId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        // findAndJoinRoom 메서드를 사용하여 게임방을 찾거나 생성합니다.
        GameRoomEntity room = gameRoomService.findAndJoinRoom(user.getId(), deckId);

        // 찾거나 생성된 방 번호로 /game/room 페이지로 리다이렉트합니다.
        return new ModelAndView("redirect:/game/room?roomNo=" + room.getNo());
    }
}