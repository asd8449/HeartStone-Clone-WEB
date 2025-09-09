package kr.ac.kopo.config;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter // JPA 컨버터로 등록
public class JpaJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    // ObjectMapper는 일반적으로 Spring Context에서 주입받지만, AttributeConverter는 Spring Bean이 아님.
    // 따라서 여기서는 정적 필드로 ObjectMapper 인스턴스를 사용합니다.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null; // DB에 null 또는 빈 문자열로 저장
        }
        try {
            return objectMapper.writeValueAsString(attribute); // Map을 JSON String으로 변환
        } catch (JsonProcessingException e) {
            log.error("Error converting Map to JSON string for DB: {}", attribute, e);
            return null; // 변환 실패 시 null 저장
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyMap(); // DB에서 null 또는 빈 문자열을 읽을 경우 빈 Map 반환
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {}); // JSON String을 Map으로 변환
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON string to Map from DB: {}", dbData, e);
            return Collections.emptyMap(); // 변환 실패 시 빈 Map 반환
        }
    }
}