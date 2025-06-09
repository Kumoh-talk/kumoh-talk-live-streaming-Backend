package com.kumoh_talk.streaming.global.socket.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Set;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.QNA_LIST_DESTINATION;

@Component
@RequiredArgsConstructor
public class StompEventListener {

    public static final String SUBSCRIBER_KEY_PREFIX = "sub:"; // sub:<destination> → Set<sessionId>
    private static final String SESSION_KEY_PREFIX = "session:"; // session:<sessionId> → Set<destination>

    private final StringRedisTemplate stringRedisTemplate;

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        // TODO. 필요 시 다른 엔드포인트로
        if (destination != null && sessionId != null && destination.startsWith(QNA_LIST_DESTINATION)) {
            stringRedisTemplate.opsForSet().add(SUBSCRIBER_KEY_PREFIX + destination, sessionId);
            stringRedisTemplate.opsForSet().add(SESSION_KEY_PREFIX + sessionId, destination);
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        // TODO. 필요 시 다른 엔드포인트로
        if (destination != null && sessionId != null && destination.startsWith(QNA_LIST_DESTINATION)) {
            stringRedisTemplate.opsForSet().remove(SUBSCRIBER_KEY_PREFIX + destination, sessionId);
            stringRedisTemplate.opsForSet().remove(SESSION_KEY_PREFIX + sessionId, destination);
        }
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        Set<String> destinations = stringRedisTemplate.opsForSet().members(SESSION_KEY_PREFIX + sessionId);
        if (destinations == null) {
            return;
        }

        destinations.forEach(destination -> {
            stringRedisTemplate.opsForSet().remove(SUBSCRIBER_KEY_PREFIX + destination, sessionId);
        });

        stringRedisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
