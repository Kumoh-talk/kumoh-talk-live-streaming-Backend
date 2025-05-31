package com.kumoh_talk.streaming.domain.chat.controller;

import com.kumoh_talk.streaming.domain.chat.dto.ChatCreateRequestDto;
import com.kumoh_talk.streaming.domain.chat.service.ChatWebSocketService;
import com.kumoh_talk.streaming.global.auth.constant.Role;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatWebSocketService chatWebSocketService;

	@MessageMapping("/streaming/{streamId}/add-chat")
	public void addChat(
		@Payload @Valid ChatCreateRequestDto chatCreateRequestDto,
		@DestinationVariable Long streamId,
		StompHeaderAccessor headerAccessor
	) {
		// TODO 로그인 구현 후 하드코딩 제거
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "하하호호", Role.ROLE_USER);
		chatWebSocketService.addChat(chatCreateRequestDto, streamId, authenticatedUser);
	}
}