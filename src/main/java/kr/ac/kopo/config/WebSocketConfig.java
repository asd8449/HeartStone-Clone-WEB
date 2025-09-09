package kr.ac.kopo.config;

import org.springframework.beans.factory.annotation.Autowired; // Autowired 임포트
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private HttpHandshakeInterceptor handshakeInterceptor; // 만든 인터셉터 주입

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 엔드포인트에 핸드셰이크 인터셉터를 추가합니다.
        registry.addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        
        registry.setUserDestinationPrefix("/user"); 
    }
    
    
}