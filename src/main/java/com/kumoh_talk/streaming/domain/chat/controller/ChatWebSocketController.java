package com.kumoh_talk.streaming.domain.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kumoh_talk.streaming.domain.chat.dto.ChatCreateRequestDto;
import com.kumoh_talk.streaming.domain.chat.service.ChatWebSocketService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
		
		private final SimpMessagingTemplate template;
		private final ChatWebSocketService chatWebSocketService;

		@MessageMapping("/streaming/{streamId}/add-chat")
		public void addChat(
				@Payload @Valid ChatCreateRequestDto chatCreateRequestDto,
				@DestinationVariable Long streamId,
				StompHeaderAccessor headerAccessor
		) {
				template.convertAndSend("/chat/streaming/" + streamId + "/add",
						chatCreateRequestDto);
				log.info("채팅 메시지 전송 - {}", chatCreateRequestDto.content());
		}
}