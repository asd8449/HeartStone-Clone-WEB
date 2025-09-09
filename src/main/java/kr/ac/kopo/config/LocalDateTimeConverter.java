package kr.ac.kopo.config;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Java의 LocalDateTime 타입을 데이터베이스의 TIMESTAMP 타입과 매핑해주는 컨버터.
 * @Converter(autoApply = true) 설정을 통해 모든 LocalDateTime 필드에 자동으로 이 컨버터가 적용됩니다.
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime locDateTime) {
        // LocalDateTime 객체가 null이 아니면, DB에 저장하기 위해 Timestamp 객체로 변환합니다.
        return (locDateTime == null ? null : Timestamp.valueOf(locDateTime));
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp sqlTimestamp) {
        // DB의 Timestamp 값이 null이 아니면, Java 엔티티에서 사용하기 위해 LocalDateTime 객체로 변환합니다.
        return (sqlTimestamp == null ? null : sqlTimestamp.toLocalDateTime());
    }
}