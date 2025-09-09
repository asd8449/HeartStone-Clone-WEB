package kr.ac.kopo.admin.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode; // JsonNode 임포트 추가
import com.fasterxml.jackson.core.type.TypeReference;


import kr.ac.kopo.card.service.CardService;
import kr.ac.kopo.card.repository.CardAbilityRepository;
import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.card.vo.CardAbilityEntity;

@Controller
public class AdminController {

	@Autowired
	private CardService cardService;

    @Autowired
    private CardAbilityRepository cardAbilityRepository;

    @Autowired
    private ObjectMapper objectMapper;
	
	@GetMapping("/admin/card")
	public ModelAndView cardlist(
		    @RequestParam(value = "minCost", required = false) Integer minCost,
		    @RequestParam(value = "sortBy",  required = false) String sortBy) {

	    List<CardVO> list = cardService.selectAllCard();

	    if ("cost".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getCost, Comparator.nullsLast(Integer::compareTo)));
	    } else if ("attack".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getAttack, Comparator.nullsLast(Integer::compareTo)));
	    } else if ("health".equals(sortBy)) {
	        list.sort(Comparator.comparing(CardVO::getHealth, Comparator.nullsLast(Integer::compareTo)));
	    }

	    ModelAndView mav = new ModelAndView("admin/adminCard");
	    mav.addObject("cardList", list);
	    mav.addObject("minCost", minCost == null ? "" : minCost);
	    mav.addObject("sortBy", sortBy == null ? "" : sortBy);
	    return mav;
	}
	@GetMapping("/admin/card/{no}")
	public ModelAndView editForm(@PathVariable("no") int no) {
		    CardVO card = cardService.selectByNo(no);

		    String csv = card.getKeyword();
		    if (csv == null || "none".equals(csv) || csv.isBlank()) {
		        csv = "";
		    }
		    Set<String> selected = new HashSet<>();
		    if (!csv.isEmpty()) {
		        selected.addAll(Arrays.asList(csv.split(",")));
		    }

		    ModelAndView mav = new ModelAndView("admin/cardModify");
		    mav.addObject("card", card);
		    // 타입 드롭다운 선택 상태 설정
            mav.addObject("selType하수인", "하수인".equals(card.getType()));
            mav.addObject("selType주문", "주문".equals(card.getType()));
            mav.addObject("selType무기", "무기".equals(card.getType()));

		    mav.addObject("sel돌진",           selected.contains("돌진"));
		    mav.addObject("sel도발",           selected.contains("도발"));
		    mav.addObject("sel은신",           selected.contains("은신"));
		    mav.addObject("sel독성",           selected.contains("독성"));
		    mav.addObject("sel천상의 보호막",  selected.contains("천상의 보호막"));
            // "전투의 함성", "죽음의 메아리" 키워드는 ABILITY_TYPE으로 관리되므로 제거
		    // mav.addObject("sel전투의 함성",    selected.contains("전투의 함성"));
		    // mav.addObject("sel죽음의 메아리",  selected.contains("죽음의 메아리"));
            // 새로 추가된 키워드들
            mav.addObject("sel생명력 흡수",    selected.contains("생명력 흡수"));
            mav.addObject("sel과부하",         selected.contains("과부하"));
            mav.addObject("sel질풍",           selected.contains("질풍"));
            mav.addObject("sel속공",           selected.contains("속공"));
            mav.addObject("sel빙결",           selected.contains("빙결"));
            mav.addObject("sel잔상",           selected.contains("잔상"));
            
            // 카드 능력 정보 조회 및 JSON 변환
            List<CardAbilityEntity> abilities = cardAbilityRepository.findByCardNo(no);
            String abilitiesJson = "[]"; // 기본값을 빈 배열 JSON 문자열로 설정 (이전: 빈 문자열)
            if (abilities != null && !abilities.isEmpty()) {
                try {
                    String rawJson = objectMapper.writeValueAsString(abilities);
                    
                    // NEW: JsonNode를 사용하여 이중 배열 문제 해결
                    JsonNode rootNode = objectMapper.readTree(rawJson);
                    
                    // 만약 rootNode가 배열이고, 그 첫 번째 요소도 배열이라면 (즉, [[...]] 형태라면)
                    if (rootNode.isArray() && !rootNode.isEmpty() && rootNode.get(0).isArray()) {
                        abilitiesJson = objectMapper.writeValueAsString(rootNode.get(0)); // 내부 배열만 다시 직렬화
                    } else {
                        abilitiesJson = rawJson; // 그 외의 경우 (정상적인 배열 또는 비배열), rawJson 그대로 사용
                    }

                    // 빈 배열로 직렬화된 경우 빈 문자열이 아닌 "[]"로 유지 (이전: 빈 문자열)
                    if ("[]".equals(abilitiesJson.trim())) { 
                        abilitiesJson = "[]"; // 수정된 부분: "" -> "[]"
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace(); 
                    abilitiesJson = "[]"; // 파싱 오류 시에도 빈 배열 JSON 문자열로 설정
                }
            }
            // 디버깅을 위해 System.out.println 추가 (최종 확인 후 제거 가능)
            System.out.println("DEBUG: abilitiesJson sent to frontend (after backend workaround v2): [" + abilitiesJson + "]"); 
            mav.addObject("abilitiesJson", abilitiesJson);

		    return mav;
	}
	@PostMapping("/admin/card/{no}")
    public String save(
            @PathVariable("no") int no,
            @RequestParam("name") String name,
            @RequestParam("type") String type,
            @RequestParam("cost") Integer cost,
            @RequestParam("attack") Integer attack,
            @RequestParam("health") Integer health,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "keywords", required = false) List<String> keywords,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "abilitiesJson", required = false) String abilitiesJson,
            RedirectAttributes redirectAttrs) {
        
        CardVO card = new CardVO();
        card.setNo(no);
        card.setName(name);
        card.setType(type);
        card.setCost(cost);
        card.setAttack(attack);
        card.setHealth(health);
        card.setDescription(description == null ? "" : description);
        card.setImageUrl(imageUrl);
        
        String csv = (keywords != null && !keywords.isEmpty())
                ? String.join(",", keywords)
                : "none";
        card.setKeyword(csv);
        
        List<CardAbilityEntity> abilities = null;
        if (abilitiesJson != null && !abilitiesJson.trim().isEmpty()) {
            try {
                abilities = objectMapper.readValue(abilitiesJson, new TypeReference<List<CardAbilityEntity>>() {});
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                redirectAttrs.addFlashAttribute("msg", "카드 능력 JSON 파싱 오류가 발생했습니다.");
                return "redirect:/admin/card/" + no;
            }
        }
        
        cardService.save(card, abilities);
        
        redirectAttrs.addFlashAttribute("msg", "카드가 정상적으로 수정되었습니다.");
        
        return "redirect:/admin/card";
    }
}