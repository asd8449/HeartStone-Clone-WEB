package kr.ac.kopo.card.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HeroVO 엔티티의 데이터를 웹(JSON)으로 안전하게 전달하기 위한 순수 데이터 객체(DTO)입니다.
 */
@Data
@NoArgsConstructor
public class HeroDTO {
    private int no;
    private String className;
    private String name;
    private String heroPowerName;
    private String heroPowerDescription;
    private int heroPowerCost;
    private String imageUrl;

    /**
     * HeroVO 엔티티 객체를 받아서 HeroDTO 객체를 생성하는 변환 생성자입니다.
     * @param entity 데이터베이스에서 조회한 HeroVO 엔티티
     */
    public HeroDTO(HeroVO entity) {
        this.no = entity.getNo();
        this.className = entity.getClassName();
        this.name = entity.getName();
        this.heroPowerName = entity.getHeroPowerName();
        this.heroPowerDescription = entity.getHeroPowerDescription();
        this.heroPowerCost = entity.getHeroPowerCost();
        this.imageUrl = entity.getImageUrl();
    }
}