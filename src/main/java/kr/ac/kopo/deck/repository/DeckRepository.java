package kr.ac.kopo.deck.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kr.ac.kopo.deck.vo.DeckEntity;

@Repository
public interface DeckRepository extends JpaRepository<DeckEntity, Integer> {
    List<DeckEntity> findByUser_Id(String userId);
}