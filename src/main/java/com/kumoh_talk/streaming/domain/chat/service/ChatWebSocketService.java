package com.kumoh_talk.streaming.domain.chat.service;

import com.kumoh_talk.streaming.domain.chat.dto.ChatCreateRequestDto;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.kumoh_talk.streaming.global.constant.WebSocketConstants.CHAT_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

    private final SimpMessagingTemplate template;

    public void addChat(ChatCreateRequestDto chatCreateRequestDto, Long streamId, AuthenticatedUser authenticatedUser) {
        template.convertAndSend(CHAT_DESTINATION + streamId + "/add",
                chatCreateRequestDto);
        log.info("채팅 메시지 전송 - {}", chatCreateRequestDto.content());
    }
}