package kr.ac.kopo.card.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map; 
import jakarta.persistence.Convert; 
import kr.ac.kopo.config.JpaJsonConverter; 
import com.fasterxml.jackson.databind.annotation.JsonDeserialize; // <-- JsonDeserialize 임포트 추가
import kr.ac.kopo.config.StringToJsonMapDeserializer; // <-- 커스텀 역직렬화기 임포트 추가

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CARD_ABILITIES") 
public class CardAbilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_card_abilities_id")
    @SequenceGenerator(name = "seq_card_abilities_id", sequenceName = "SEQ_CARD_ABILITIES_ID", allocationSize = 1)
    @Column(name = "ABILITY_ID")
    private Long abilityId; 

    @Column(name = "CARD_NO")
    private Integer cardNo; 

    @Column(name = "TRIGGER_EVENT")
    private String triggerEvent;

    @Column(name = "EFFECT_TYPE")
    private String effectType;

    // JPA Persistence layer converter (DB 저장/조회 시 사용)
    @Convert(converter = JpaJsonConverter.class) 
    // Jackson Deserialization layer (JSON -> Java Object 변환 시 사용)
    @JsonDeserialize(using = StringToJsonMapDeserializer.class) // <-- 커스텀 역직렬화기 적용
    @Column(name = "EFFECT_PARAMS_JSON", columnDefinition = "CLOB") 
    private Map<String, Object> effectParamsJson; 

    @Column(name = "TARGET_SCOPE")
    private String targetScope;
}