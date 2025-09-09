package kr.ac.kopo.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketEventListener {

    // 세션ID와 대기 중인 방 번호를 1:1로 저장하는 동시성 맵
    private final Map<String, Integer> waitingSessions = new ConcurrentHashMap<>();

    @Autowired
    private GameRoomService gameRoomService;

    /**
     * 사용자가 특정 채널을 "구독"할 때마다 호출되는 이벤트 리스너입니다.
     * 이 프로젝트에서는 사용자가 대기방에 들어가면 '/topic/game/{roomNo}' 채널을 구독합니다.
     * 이 순간을 포착하여 "어떤 세션이 몇 번 방에서 대기 중"인지 기록합니다.
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();

        if (destination != null && destination.startsWith("/topic/game/")) {
            try {
                String[] parts = destination.split("/");
                // 순수한 게임방 주소("/topic/game/{방번호}")만 처리
                if (parts.length == 4) {
                    Integer roomNo = Integer.parseInt(parts[3]);
                    
                    if (gameRoomService.isWaitingRoom(roomNo)) {
                        // 이 부분은 이제 직접적인 방 정리에 사용되지는 않지만,
                        // 추후 운영/디버깅을 위해 세션 추적 기능을 남겨둡니다.
                        waitingSessions.put(sessionId, roomNo); 
                        log.info("### [세션 등록] 세션 ID: {}, 대기방 번호: {}", sessionId, roomNo);
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("대기방 구독 주소에서 방 번호를 파싱하는 데 실패했습니다: {}", destination);
            }
        }
    }

}