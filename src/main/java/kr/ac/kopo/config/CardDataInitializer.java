package kr.ac.kopo.config;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.ac.kopo.card.repository.CardRepository;
import kr.ac.kopo.card.repository.HeroRepository;
import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.card.vo.HeroVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 이전에 주석 처리 하셨다면, DB 업데이트를 위해 잠시 주석을 해제하고 실행해주세요.
public class CardDataInitializer implements CommandLineRunner {

    @Autowired
    private CardRepository cardRepository;
    
    @Autowired
    private HeroRepository heroRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("DataInitializer 시작: DB 카드 및 영웅 정보 자동 업데이트를 확인합니다...");
        updateCardData();
        updateHeroData();
    }
    
    private void updateHeroData() {
        List<HeroVO> allDbHeroes = heroRepository.findAll();
        
        // 영웅의 imageUrl이 하나라도 비어있으면 업데이트 실행
        if (!allDbHeroes.isEmpty() && allDbHeroes.stream().anyMatch(h -> h.getImageUrl() == null)) {
            log.info("DB 영웅 정보 업데이트를 시작합니다...");

            // 하스스톤 기본 영웅 9명의 이름과 공식 ID를 매핑합니다.
            Map<String, String> heroIdMap = Map.of(
                "제이나 프라우드무어", "HERO_08",
                "렉사르", "HERO_05",
                "우서 라이트브링어", "HERO_04",
                "가로쉬 헬스크림", "HERO_01",
                "말퓨리온 스톰레이지", "HERO_06",
                "굴단", "HERO_07",
                "안두인 린", "HERO_09",
                "발리라 생귀나르", "HERO_03",
                "스랄", "HERO_02"
            );

            for (HeroVO hero : allDbHeroes) {
                String heroId = heroIdMap.get(hero.getName());
                if (heroId != null) {
                    // *** 바로 이 부분이 사용자님의 요구사항에 맞춰 최종 수정된 코드입니다 ***
                    String imageUrl = String.format("https://art.hearthstonejson.com/v1/orig/%s.png", heroId);
                    hero.setImageUrl(imageUrl);
                } else {
                    log.warn("'{}' 영웅의 이미지 ID를 찾지 못했습니다.", hero.getName());
                }
            }
            heroRepository.saveAll(allDbHeroes);
            log.info("영웅 이미지 URL 업데이트를 완료했습니다.");
        } else {
            log.info("모든 영웅에 이미 imageUrl이 설정되어 있거나, DB에 영웅이 없습니다.");
        }
    }

    private void updateCardData() throws Exception {
        List<CardVO> allDbCards = cardRepository.findAll();

        if (!allDbCards.isEmpty() && allDbCards.stream().anyMatch(c -> c.getCardId() == null)) {
            log.info("HearthstoneJSON API로부터 최신 카드 데이터를 스트리밍 방식으로 가져옵니다...");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.hearthstonejson.com/v1/latest/koKR/cards.collectible.json"))
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.error("HearthstoneJSON API 호출에 실패했습니다. 상태 코드: " + response.statusCode());
                response.body().close();
                return;
            }

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            MappingIterator<ApiCard> iterator = mapper.readerFor(ApiCard.class).readValues(response.body());
            int updatedCount = 0;
            Map<String, CardVO> dbCardMap = allDbCards.stream()
                .collect(Collectors.toMap(CardVO::getName, Function.identity(), (c1, c2) -> c1));

            log.info("API 데이터를 처리하며 DB 카드 정보 업데이트를 시작합니다...");

            while (iterator.hasNext()) {
                ApiCard apiCard = iterator.next();
                CardVO dbCard = dbCardMap.get(apiCard.getName());

                if (dbCard != null) {
                    boolean needsUpdate = false;
                    if (dbCard.getCardId() == null) {
                        dbCard.setCardId(apiCard.getId());
                        needsUpdate = true;
                    }
                    
                    String newImageUrl = String.format("https://art.hearthstonejson.com/v1/orig/%s.png", apiCard.getId());
                    if (dbCard.getImageUrl() == null || !dbCard.getImageUrl().equals(newImageUrl)) {
                        dbCard.setImageUrl(newImageUrl);
                        needsUpdate = true;
                    }
                    if(needsUpdate) updatedCount++;
                }
            }
            
            if (updatedCount > 0) {
                cardRepository.saveAll(dbCardMap.values());
                log.info("총 {}개의 카드 정보(card_id, image_url)를 성공적으로 업데이트했습니다.", updatedCount);
            } else {
                log.info("업데이트할 새로운 카드 정보가 없습니다.");
            }
            
            response.body().close();
        }
    }

    @Data
    private static class ApiCard {
        private String id;
        private String name;
    }
}