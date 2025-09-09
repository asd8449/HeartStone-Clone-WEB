package kr.ac.kopo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

// ObjectMapper는 JsonDeserializer 안에서 직접 주입받기 어려워 static으로 인스턴스화합니다.
public class StringToJsonMapDeserializer extends JsonDeserializer<Map<String, Object>> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // 들어오는 JSON 토큰의 타입을 확인합니다.
        // 만약 직접적인 JSON 객체 (예: { "amount": 1 }) 라면 그대로 읽습니다.
        if (p.isExpectedStartObjectToken()) {
            return mapper.readValue(p, new TypeReference<Map<String, Object>>() {});
        }
        
        // 만약 JSON 문자열 (예: "{ \"amount\": 1 }") 이라면, 문자열을 가져와 다시 JSON으로 파싱합니다.
        String jsonString = p.getText(); 
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // 문자열을 다시 JSON 객체로 파싱 시도
            return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            // 파싱 실패 시 (유효하지 않은 JSON 문자열 등) 오류 처리
            ctxt.reportInputMismatch(Map.class, "Cannot deserialize JSON string '%s' into Map: %s", jsonString, e.getMessage());
            return Collections.emptyMap(); 
        }
    }
}