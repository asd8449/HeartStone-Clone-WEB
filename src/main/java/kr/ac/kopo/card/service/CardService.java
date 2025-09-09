package kr.ac.kopo.card.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // @Transactional 임포트 추가

import kr.ac.kopo.card.repository.CardRepository;
import kr.ac.kopo.card.repository.CardAbilityRepository; // CardAbilityRepository 임포트 추가
import kr.ac.kopo.card.vo.CardVO;
import kr.ac.kopo.card.vo.CardAbilityEntity; // CardAbilityEntity 임포트 추가

@Service
public class CardService {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private CardAbilityRepository cardAbilityRepository; // CardAbilityRepository 주입
	
	public List<CardVO> selectAllCard(){
		return cardRepository.findAll();
	}

	public CardVO selectByNo(int no) {
		return cardRepository.findById(no).get();
	}

	@Transactional // 트랜잭션 처리
	public void save(CardVO card) {
		// 기본 카드 정보 저장/업데이트
		cardRepository.save(card);
	}

	@Transactional // 카드 능력 저장/업데이트를 위한 오버로드 메서드 또는 별도 메서드
	public void save(CardVO card, List<CardAbilityEntity> abilities) {
		// 1. 기본 카드 정보 저장/업데이트
		cardRepository.save(card);

		// 2. 기존 능력 정보 삭제 (해당 카드의 모든 능력 삭제)
		cardAbilityRepository.deleteByCardNo(card.getNo());

		// 3. 새로운 능력 정보 저장
		if (abilities != null && !abilities.isEmpty()) {
			for (CardAbilityEntity ability : abilities) {
				ability.setCardNo(card.getNo()); // 저장될 카드의 번호 설정
			}
			cardAbilityRepository.saveAll(abilities);
		}
	}
}