package com.kumoh_talk.streaming.domain.qna.controller;

import com.kumoh_talk.streaming.domain.qna.dto.request.QnaCreateRequest;
import com.kumoh_talk.streaming.domain.qna.service.QnaWebSocketService;
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
public class QnaWebSocketController {

	private final QnaWebSocketService qnaWebSocketService;

	@MessageMapping("/streaming/{streamId}/add-qna")
	public void addQna(
		@Payload @Valid QnaCreateRequest qnaCreateRequestDto,
		@DestinationVariable Long streamId,
		StompHeaderAccessor headerAccessor
	) {
		// TODO 로그인 구현 후 하드코딩 제거
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "하하호호", Role.ROLE_USER);
		qnaWebSocketService.addQna(qnaCreateRequestDto, streamId, authenticatedUser);
	}

	@MessageMapping("/streaming/{streamId}/liked-qna/{qnaId}")
	public void likeQna(
		@DestinationVariable Long streamId,
		@DestinationVariable Long qnaId,
		StompHeaderAccessor headerAccessor
	) {
		// TODO 로그인 구현 후 하드코딩 제거
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "하하호호", Role.ROLE_USER);
		qnaWebSocketService.likeQna(streamId, qnaId, authenticatedUser);
	}

	@MessageMapping("/streaming/{streamId}/delete-qna/{qnaId}")
	public void deleteQna(
		@DestinationVariable Long streamId,
		@DestinationVariable Long qnaId,
		StompHeaderAccessor headerAccessor
	) {
		// TODO 로그인 구현 후 하드코딩 제거
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "하하호호", Role.ROLE_ADMIN);
		qnaWebSocketService.deleteQna(streamId, qnaId, authenticatedUser);
	}
}