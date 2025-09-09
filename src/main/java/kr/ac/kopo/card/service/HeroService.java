package kr.ac.kopo.card.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import kr.ac.kopo.card.repository.HeroRepository;
import kr.ac.kopo.card.vo.HeroVO;

@Service
public class HeroService {

    @Autowired
    private HeroRepository heroRepository;

    public List<HeroVO> getAllHeroes() {
        return heroRepository.findAll();
    }

    public HeroVO getHeroById(int heroId) {
        return heroRepository.findById(heroId).orElse(null);
    }
}