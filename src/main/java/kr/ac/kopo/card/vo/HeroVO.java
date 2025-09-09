package kr.ac.kopo.card.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "A_HERO")
public class HeroVO {

    @Id
    private int no;

    @Column(name = "class_name")
    private String className;

    private String name;
    
    @Column(name = "hero_power_name")
    private String heroPowerName;

    @Column(name = "hero_power_description")
    private String heroPowerDescription;
    
    // [추가] 영웅 능력 비용
    @Column(name = "hero_power_cost")
    private int heroPowerCost;
    
    @Column(name="image_url")
    private String imageUrl;
}