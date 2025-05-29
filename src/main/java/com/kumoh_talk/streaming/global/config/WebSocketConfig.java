package com.kumoh_talk.streaming.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // prefix /app 으로 들어오는 메시지는 @MessageMapping 으로 라우팅
        registry.setApplicationDestinationPrefixes("/app");
        // 간단한 메모리 브로커를 켜고, /chat, /qna로 구독 처리
        registry.enableSimpleBroker("/chat", "/qna");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 엔드포인트
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS fallback 허용

        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }
}