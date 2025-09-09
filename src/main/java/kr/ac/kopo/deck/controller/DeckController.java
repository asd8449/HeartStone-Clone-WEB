package kr.ac.kopo.deck.controller;

import java.util.List;
import java.util.stream.Collectors; // Collectors 임포트 추가
import java.util.Arrays; // Arrays 임포트 추가

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.card.service.CardService;
import kr.ac.kopo.card.service.HeroService;
import kr.ac.kopo.card.vo.HeroVO;
import kr.ac.kopo.deck.service.DeckService;
import kr.ac.kopo.deck.vo.DeckEntity;
import kr.ac.kopo.game.vo.DeckDTO;
import kr.ac.kopo.user.vo.UserEntity;

@Controller
public class DeckController {

    @Autowired private HeroService heroService;
    @Autowired private DeckService deckService;
    @Autowired private CardService cardService;
    @Autowired private ObjectMapper mapper;
    
    @GetMapping("/deck")
    public ModelAndView myDecks(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return new ModelAndView("redirect:/login");
        List<DeckEntity> decks = deckService.getDecksByUserId(user.getId());
        ModelAndView mav = new ModelAndView("deck/myDecks");
        mav.addObject("decks", decks);
        mav.addObject("user", user);
        return mav;
    }

    @GetMapping("/deck/new")
    public ModelAndView selectHero(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return new ModelAndView("redirect:/login");
        
        List<HeroVO> allHeroes = heroService.getAllHeroes(); // 모든 영웅을 가져옴
        
        // 영웅 능력이 구현된 영웅들만 필터링
        List<String> implementedHeroClasses = Arrays.asList("마법사", "흑마법사", "사냥꾼", "사제");
        List<HeroVO> filteredHeroList = allHeroes.stream()
                                            .filter(hero -> implementedHeroClasses.contains(hero.getClassName()))
                                            .collect(Collectors.toList());

        ModelAndView mav = new ModelAndView("deck/selectHero");
        mav.addObject("heroes", filteredHeroList); // 필터링된 목록을 전달
        mav.addObject("user", user);
        return mav;
    }
    
    @GetMapping("/deck/build/{heroNo}")
    public ModelAndView deckBuilder(@PathVariable("heroNo") int heroNo, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return new ModelAndView("redirect:/login");
        ModelAndView mav = new ModelAndView("deck/deckBuilder");
        mav.addObject("hero", heroService.getHeroById(heroNo));
        mav.addObject("allCards", cardService.selectAllCard());
        mav.addObject("user", user);
        return mav;
    }
    
    @PostMapping("/deck/save")
    public ResponseEntity<?> saveDeck(@RequestBody DeckSaveRequest data, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        if (data.getCardIds().size() != 30) return ResponseEntity.badRequest().body("덱은 반드시 30장으로 구성되어야 합니다.");
        DeckEntity deck = deckService.createDeck(user, data.getDeckName());
        deckService.saveDeckCards(deck.getNo(), data.getCardIds());
        return ResponseEntity.ok().body(java.util.Map.of("message", "덱이 성공적으로 저장되었습니다.", "deckId", deck.getNo()));
    }
    
    @GetMapping("/deck/edit/{deckId}")
    public ModelAndView deckEditor(@PathVariable("deckId") int deckId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        DeckDTO deck = deckService.getDeckWithCards(deckId);

        if (user == null || deck == null || !deck.getUserId().equals(user.getId())) {
            return new ModelAndView("redirect:/deck");
        }

        ModelAndView mav = new ModelAndView("deck/deckEditor");
        mav.addObject("deck", deck);
        mav.addObject("allCards", cardService.selectAllCard());
        mav.addObject("user", user);
        
        try {
            String cardsJson = mapper.writeValueAsString(deck.getCards());
            mav.addObject("cardsJson", cardsJson);
        } catch (JsonProcessingException e) {
            mav.addObject("cardsJson", "[]");
        }

        return mav;
    }

    @PostMapping("/deck/update/{deckId}")
    public ResponseEntity<?> updateDeck(@PathVariable("deckId") int deckId, @RequestBody DeckSaveRequest data, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        if (data.getCardIds().size() != 30) return ResponseEntity.badRequest().body("덱은 반드시 30장으로 구성되어야 합니다.");
        deckService.updateDeck(deckId, data.getDeckName(), data.getCardIds());
        return ResponseEntity.ok().body(java.util.Map.of("message", "덱이 성공적으로 수정되었습니다."));
    }

    @PostMapping("/deck/delete/{deckId}")
    public String deleteDeck(@PathVariable("deckId") int deckId, HttpSession session, RedirectAttributes redirectAttrs) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        deckService.deleteDeck(deckId);
        redirectAttrs.addFlashAttribute("message", "덱이 삭제되었습니다.");
        return "redirect:/deck";
    }

    static class DeckSaveRequest {
        private String deckName;
        private List<Integer> cardIds;
        public String getDeckName() { return deckName; }
        public void setDeckName(String deckName) { this.deckName = deckName; }
        public List<Integer> getCardIds() { return cardIds; }
        public void setCardIds(List<Integer> cardIds) { this.cardIds = cardIds; }
    }
}