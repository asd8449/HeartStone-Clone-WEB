package kr.ac.kopo.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import kr.ac.kopo.card.vo.CardVO;

@Repository
public interface CardRepository extends JpaRepository <CardVO, Integer> {

}
