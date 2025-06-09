package com.kumoh_talk.streaming.global.socket.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.SESSION_KEY_PREFIX;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.SUBSCRIBER_KEY_PREFIX;

@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final StringRedisTemplate stringRedisTemplate;

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        Set<String> streamIds = stringRedisTemplate.opsForSet().members(SESSION_KEY_PREFIX + sessionId);
        if (streamIds == null) {
            return;
        }

        streamIds.forEach(streamId -> {
            stringRedisTemplate.opsForSet().remove(SUBSCRIBER_KEY_PREFIX + streamId, sessionId);
        });

        stringRedisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
