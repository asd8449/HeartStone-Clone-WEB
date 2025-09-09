package kr.ac.kopo.deck.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.kopo.card.repository.CardAbilityRepository; // CardAbilityRepository 임포트 추가
import kr.ac.kopo.card.repository.CardRepository;
import kr.ac.kopo.card.vo.CardAbilityEntity; // CardAbilityEntity 임포트 추가
import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.deck.repository.CardOfDeckRepository;
import kr.ac.kopo.deck.repository.DeckRepository;
import kr.ac.kopo.deck.vo.CardOfDeckEntity;
import kr.ac.kopo.deck.vo.CardOfDeckId;
import kr.ac.kopo.deck.vo.DeckEntity;
import kr.ac.kopo.game.vo.CardDTO;
import kr.ac.kopo.game.vo.DeckDTO;
import kr.ac.kopo.user.vo.UserEntity;

@Service
public class DeckService {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardOfDeckRepository cardOfDeckRepository;
    
    @Autowired
    private CardRepository cardRepository;

    @Autowired // CardAbilityRepository 의존성 주입
    private CardAbilityRepository cardAbilityRepository;

    @Transactional
    public DeckEntity createDeck(UserEntity user, String deckName) {
        DeckEntity newDeck = new DeckEntity();
        newDeck.setUser(user);
        newDeck.setName(deckName);
        return deckRepository.save(newDeck);
    }
    
    @Transactional
    public void saveDeckCards(int deckId, List<Integer> cardIds) {
        cardOfDeckRepository.deleteByIdDeckId(deckId);
        var cardQuantities = cardIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<CardOfDeckEntity> cardsOfDeck = cardQuantities.entrySet().stream().map(entry -> {
            CardOfDeckId id = new CardOfDeckId(deckId, entry.getKey());
            return new CardOfDeckEntity(id, entry.getValue().intValue());
        }).collect(Collectors.toList());
        cardOfDeckRepository.saveAll(cardsOfDeck);
    }
    
    @Transactional
    public void updateDeck(int deckId, String deckName, List<Integer> cardIds) {
        DeckEntity deck = deckRepository.findById(deckId).orElseThrow(() -> new IllegalArgumentException("Invalid deck Id:" + deckId));
        deck.setName(deckName);
        deckRepository.save(deck);
        saveDeckCards(deckId, cardIds);
    }

    @Transactional
    public void deleteDeck(int deckId) {
        cardOfDeckRepository.deleteByIdDeckId(deckId);
        deckRepository.deleteById(deckId);
    }
    
    public List<DeckEntity> getDecksByUserId(String userId) {
        return deckRepository.findByUser_Id(userId);
    }
    
    @Transactional(readOnly = true)
    public DeckDTO getDeckWithCards(int deckId) {
        DeckEntity deckEntity = deckRepository.findById(deckId).orElse(null);
        if (deckEntity == null) return null;

        List<CardOfDeckEntity> cardsInDeck = cardOfDeckRepository.findById_DeckId(deckId);
        
        List<Integer> uniqueCardIds = cardsInDeck.stream()
                                                 .map(cod -> cod.getId().getCardId())
                                                 .collect(Collectors.toList());
        
        Map<Integer, CardVO> cardVoMap = cardRepository.findAllById(uniqueCardIds).stream()
                                                       .collect(Collectors.toMap(CardVO::getNo, Function.identity()));
        
        // 카드 능력 정보를 미리 조회하여 Map에 저장
        Map<Integer, List<CardAbilityEntity>> cardAbilitiesMap = uniqueCardIds.stream()
                .collect(Collectors.toMap(
                    Function.identity(), // 카드 번호 (Integer)를 키로
                    cardAbilityRepository::findByCardNo // 카드 번호로 능력 목록 조회 (List<CardAbilityEntity>)
                ));

        List<CardDTO> finalCardList = new ArrayList<>();
        cardsInDeck.forEach(cardOfDeck -> {
            CardVO cardInfo = cardVoMap.get(cardOfDeck.getId().getCardId());
            if (cardInfo != null) {
                List<CardAbilityEntity> abilities = cardAbilitiesMap.getOrDefault(cardInfo.getNo(), Collections.emptyList());
                for (int i = 0; i < cardOfDeck.getQuantity(); i++) {
                    finalCardList.add(new CardDTO(cardInfo, abilities)); // CardDTO에 능력 목록 전달
                }
            }
        });

        // List<CardDTO> cardDtos는 이제 abilities 정보를 포함합니다.
        // finalCardList 자체가 이미 CardDTO 객체들로 구성됩니다.
        // List<CardDTO> cardDtos = finalCardList.stream()
        //                                       .map(CardDTO::new) // 더 이상 CardVO -> CardDTO 변환 불필요
        //                                       .collect(Collectors.toList());


        DeckDTO dto = new DeckDTO();
        dto.setNo(deckEntity.getNo());
        dto.setName(deckEntity.getName());
        dto.setUserId(deckEntity.getUser().getId());
        dto.setRegDate(deckEntity.getRegDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setCards(finalCardList); // finalCardList는 이제 CardDTO 목록입니다.
        
        return dto;
    }
}