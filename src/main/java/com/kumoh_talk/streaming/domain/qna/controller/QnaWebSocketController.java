package com.kumoh_talk.streaming.domain.qna.controller;

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
import com.kumoh_talk.streaming.domain.qna.dto.QnaCreateRequestDto;
import com.kumoh_talk.streaming.domain.qna.dto.QnaDeleteRequestDto;
import com.kumoh_talk.streaming.domain.qna.service.QnaWebSocketService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QnaWebSocketController {
		
		private final SimpMessagingTemplate template;
		private final QnaWebSocketService qnaWebSocketService;

		@MessageMapping("/streaming/{streamId}/add-qna")
		public void addQna(
				@Payload @Valid QnaCreateRequestDto qnaCreateRequestDto,
				@DestinationVariable Long streamId,
				StompHeaderAccessor headerAccessor
		) {
				template.convertAndSend("/qna/streaming/" + streamId + "/add",
						qnaCreateRequestDto);
				log.info("qna 메시지 전송 - {}", qnaCreateRequestDto.content());
		}

		@MessageMapping("/streaming/{streamId}/liked-qna")
		public void likedQna(
				@Payload @Valid QnaLikedRequestDto qnaLikedRequestDto,
				@DestinationVariable Long streamId,
				StompHeaderAccessor headerAccessor
		) {
				template.convertAndSend("/qna/streaming/" + streamId + "/liked",
						qnaLikedRequestDto);
				log.info("qna 메시지 전송 - {}", qnaLikedRequestDto.qnaId());
		}

		@MessageMapping("/streaming/{streamId}/delete-qna")
		public void deleteQna(
				@Payload @Valid QnaDeleteRequestDto qnaDeleteRequestDto,
				@DestinationVariable Long streamId,
				StompHeaderAccessor headerAccessor
		) {
				template.convertAndSend("/qna/streaming/" + streamId + "/delete",
						qnaDeleteRequestDto);
				log.info("qna 메시지 삭제 - {}", qnaDeleteRequestDto.qnaId());
		}
}