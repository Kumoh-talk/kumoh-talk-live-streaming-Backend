package com.kumoh_talk.streaming.domain.qna.controller;

import com.kumoh_talk.streaming.domain.qna.dto.request.QnaCreateRequest;
import com.kumoh_talk.streaming.domain.qna.service.QnaWebSocketService;
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
public class QnaWebSocketController {

	private final WebSocketAuthValidator webSocketAuthValidator;
	private final QnaWebSocketService qnaWebSocketService;

	@MessageMapping("/streaming/{streamId}/qna-list")
	public void getQnaList(
			@DestinationVariable Long streamId,
			StompHeaderAccessor headerAccessor
	) {
		qnaWebSocketService.getQnaList(streamId, headerAccessor.getSessionId());
	}

	@MessageMapping("/streaming/{streamId}/add-qna")
	public void addQna(
		@Payload @Valid QnaCreateRequest qnaCreateRequestDto,
		@DestinationVariable Long streamId,
		StompHeaderAccessor headerAccessor
	) {
		AuthenticatedUser authenticatedUser =
				(AuthenticatedUser) webSocketAuthValidator.validateTokenAndRole(headerAccessor, Role.ROLE_USER.name());
		qnaWebSocketService.addQna(qnaCreateRequestDto, streamId, authenticatedUser);
	}

	@MessageMapping("/streaming/{streamId}/liked-qna/{qnaId}")
	public void likeQna(
		@DestinationVariable Long streamId,
		@DestinationVariable Long qnaId,
		StompHeaderAccessor headerAccessor
	) {
		AuthenticatedUser authenticatedUser =
				(AuthenticatedUser) webSocketAuthValidator.validateTokenAndRole(headerAccessor, Role.ROLE_USER.name());
		qnaWebSocketService.likeQna(streamId, qnaId, authenticatedUser);
	}

	@MessageMapping("/streaming/{streamId}/delete-qna/{qnaId}")
	public void deleteQna(
		@DestinationVariable Long streamId,
		@DestinationVariable Long qnaId,
		StompHeaderAccessor headerAccessor
	) {
		AuthenticatedUser authenticatedUser =
				(AuthenticatedUser) webSocketAuthValidator.validateTokenAndRole(headerAccessor, Role.ROLE_ADMIN.name());
		qnaWebSocketService.deleteQna(streamId, qnaId, authenticatedUser);
	}
}