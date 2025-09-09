package kr.ac.kopo.card.repository;

import java.util.List; // List 임포트 추가

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kr.ac.kopo.card.vo.HeroVO;

@Repository
public interface HeroRepository extends JpaRepository<HeroVO, Integer> {

    // 이 메소드를 추가합니다.
    List<HeroVO> findByClassName(String className);
    
}