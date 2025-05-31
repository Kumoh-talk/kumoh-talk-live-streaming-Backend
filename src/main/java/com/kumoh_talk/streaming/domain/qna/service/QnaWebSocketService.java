package com.kumoh_talk.streaming.domain.qna.service;

import com.kumoh_talk.streaming.domain.qna.dto.request.QnaCreateRequest;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.kumoh_talk.streaming.global.constant.WebSocketConstants.QNA_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaWebSocketService {

    private final SimpMessagingTemplate template;

    public void addQna(QnaCreateRequest qnaCreateRequestDto, Long streamId, AuthenticatedUser authenticatedUser) {
        template.convertAndSend(QNA_DESTINATION + streamId + "/add", qnaCreateRequestDto);
        log.info("qna 메시지 전송 - {}", qnaCreateRequestDto.content());
    }

    public void likeQna(Long streamId, Long qnaId, AuthenticatedUser authenticatedUser) {
        template.convertAndSend(QNA_DESTINATION + streamId + "/liked", qnaId);
        log.info("qna 메시지 좋아요 - {}", qnaId);
    }

    public void deleteQna(Long streamId, Long qnaId, AuthenticatedUser authenticatedUser) {
        template.convertAndSend(QNA_DESTINATION + streamId + "/delete", qnaId);
        log.info("qna 메시지 삭제 - {}", qnaId);
    }
}