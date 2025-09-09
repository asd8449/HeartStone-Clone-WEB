package kr.ac.kopo.card.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kr.ac.kopo.card.service.CardService;
import kr.ac.kopo.card.vo.CardVO;

@Controller
public class CardController {
	
	@Autowired
	private CardService cardService;
	
	@GetMapping("/card")
	public ModelAndView list(
		    @RequestParam(value = "minCost", required = false) Integer minCost,
		    @RequestParam(value = "sortBy",  required = false) String sortBy) {

	    List<CardVO> list = cardService.selectAllCard();

	    // 1) 비용 필터
	    if (minCost != null) {
	        list = list.stream()
	                   .filter(c -> c.getCost() >= minCost)
	                   .collect(Collectors.toList());
	    }

	    // 2) 정렬
	    if ("cost".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getCost, Comparator.nullsLast(Integer::compareTo)));
	    } else if ("attack".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getAttack, Comparator.nullsLast(Integer::compareTo)));
	    } else if ("health".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getHealth, Comparator.nullsLast(Integer::compareTo)));
	    }

	    ModelAndView mav = new ModelAndView("card/cardList");
	    mav.addObject("cardList", list);
	    mav.addObject("minCost", minCost == null ? "" : minCost);
	    mav.addObject("sortBy", sortBy == null ? "" : sortBy);
	    return mav;
	}
	
	@GetMapping("/card/{no}")
	public ModelAndView detail(@PathVariable("no") int no) {
	    ModelAndView mav = new ModelAndView("card/detail");
	    CardVO card = cardService.selectByNo(no);
	    
	    // CSV → 리스트
	    String csv = card.getKeyword();
	    List<String> keywords = new ArrayList<>();
	    if (csv != null && !"none".equals(csv) && !csv.isBlank()) {
	        keywords = Arrays.stream(csv.split(","))
	                         .filter(s -> !s.isBlank())
	                         .collect(Collectors.toList());
	    }

	    mav.addObject("card", card);
	    mav.addObject("keywords", keywords);
	    return mav;
	}
}
