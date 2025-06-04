package com.kumoh_talk.streaming.domain.chat.controller;

import com.kumoh_talk.streaming.domain.chat.dto.request.ChatCreateRequest;
import com.kumoh_talk.streaming.domain.chat.service.ChatWebSocketService;
import com.kumoh_talk.streaming.global.auth.constant.Role;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.socket.security.WebSocketAuthValidator;
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

	private final WebSocketAuthValidator webSocketAuthValidator;
	private final ChatWebSocketService chatWebSocketService;

	@MessageMapping("/streaming/{streamId}/add-chat")
	public void addChat(
		@Payload @Valid ChatCreateRequest chatCreateRequest,
		@DestinationVariable Long streamId,
		StompHeaderAccessor headerAccessor
	) {
		AuthenticatedUser authenticatedUser =
				(AuthenticatedUser) webSocketAuthValidator.validateTokenAndRole(headerAccessor, Role.ROLE_USER.name());
		chatWebSocketService.addChat(chatCreateRequest, streamId, authenticatedUser);
	}
}