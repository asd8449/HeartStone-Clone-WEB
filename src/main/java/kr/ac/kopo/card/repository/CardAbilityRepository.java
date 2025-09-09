package kr.ac.kopo.card.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // deleteByCardNo에 필요

import kr.ac.kopo.card.vo.CardAbilityEntity;

@Repository
public interface CardAbilityRepository extends JpaRepository<CardAbilityEntity, Long> {
    // 특정 카드 번호에 해당하는 모든 능력 조회
    List<CardAbilityEntity> findByCardNo(Integer cardNo);

    // 특정 카드 번호에 해당하는 모든 능력 삭제
    @Transactional
    void deleteByCardNo(Integer cardNo);
}