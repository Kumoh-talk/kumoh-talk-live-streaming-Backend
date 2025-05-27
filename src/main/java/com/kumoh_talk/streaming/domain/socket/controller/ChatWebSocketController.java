package com.kumoh_talk.streaming.domain.socket.controller;

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
import com.kumoh_talk.streaming.domain.socket.dto.ChatCreateRequestDto;
import com.kumoh_talk.streaming.domain.socket.service.ChatWebSocketService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
		
		private final SimpMessagingTemplate template;
		private final ChatWebSocketService chatWebSocketService;

		@MessageMapping("/streaming/{streamId}")
		public void sendMessage(
				@Payload @Valid ChatCreateRequestDto chatCreateRequestDto
		) {
				template.convertAndSend("/chat/streaming/1/messages",
						chatCreateRequestDto);
				log.info("채팅 메시지 전송 - {}", chatCreateRequestDto.content());

				template.convertAndSend("/chat/streaming/1");
				log.info("팀스페이스 전역 채팅 메시지 수신 - 팀스페이스 Id: 1, 수신 메시지 - {}", chatCreateRequestDto.content());
		}
}