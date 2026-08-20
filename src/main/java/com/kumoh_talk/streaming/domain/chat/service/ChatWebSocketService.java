package com.kumoh_talk.streaming.domain.chat.service;

import com.kumoh_talk.streaming.domain.chat.dto.request.ChatCreateRequest;
import com.kumoh_talk.streaming.domain.chat.dto.response.ChatResponse;
import com.kumoh_talk.streaming.domain.chat.persistent.entity.Chat;
import com.kumoh_talk.streaming.domain.chat.persistent.repository.ChatRepository;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.CHAT_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

    private final SimpMessagingTemplate template;
    private final ChatRepository chatRepository;

    @Transactional
    public void addChat(ChatCreateRequest chatCreateRequest, Long streamId, AuthenticatedUser authenticatedUser) {
        // TODO. 스트리밍 시작 시 VOD를 미리 생성(VOD의 상태 추가 필요할 수도)하고 streamId-vodId 매핑을 캐싱한 뒤 Chat-Vod 연관관계를 저장
        Chat chat = Chat.builder()
                .content(chatCreateRequest.content())
                .userId(authenticatedUser.userId())
                .nickname(authenticatedUser.nickname())
                .build();

        Chat savedChat = chatRepository.save(chat);

        ChatResponse response = ChatResponse.builder()
                .userId(savedChat.getUserId())
                .nickname(savedChat.getNickname())
                .chatId(savedChat.getId())
                .content(savedChat.getContent())
                .build();

        template.convertAndSend(CHAT_DESTINATION + streamId + "/add", response);
        log.info("채팅 메시지 전송 - {}: {}", response.userId(), response.content());
    }
}
