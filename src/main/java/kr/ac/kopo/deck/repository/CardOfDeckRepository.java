package kr.ac.kopo.deck.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.kopo.deck.vo.CardOfDeckEntity;
import kr.ac.kopo.deck.vo.CardOfDeckId;

@Repository
public interface CardOfDeckRepository extends JpaRepository<CardOfDeckEntity, CardOfDeckId> {
    List<CardOfDeckEntity> findById_DeckId(int deckId);
    
    // 이 메소드를 추가합니다.
    // deckId를 기준으로 관련된 모든 카드 데이터를 삭제합니다.
    @Transactional
    void deleteByIdDeckId(int deckId);
}