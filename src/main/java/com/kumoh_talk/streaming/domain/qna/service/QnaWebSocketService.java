package com.kumoh_talk.streaming.domain.qna.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumoh_talk.streaming.domain.qna.dto.request.QnaCreateRequest;
import com.kumoh_talk.streaming.domain.qna.dto.response.QnaLikeResponse;
import com.kumoh_talk.streaming.domain.qna.dto.response.QnaListResponse;
import com.kumoh_talk.streaming.domain.qna.redis.entity.Qna;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.QNA_DESTINATION;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.QNA_LIST_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaWebSocketService {

    private static final String QNA_KEY = "qna:";
    private static final String QNA_ID_KEY = "qna:id:seq";
    private static final String QNA_LIKE_SET_KEY = "qna:like:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate template;

    public void getQnaList(Long streamId, String sessionId) {
        List<Object> values = stringRedisTemplate.opsForHash().values(QNA_KEY + streamId);

        List<QnaListResponse.QnaInfo> qnaInfos = values.stream()
                .map(o -> {
                    try {
                        return objectMapper.readValue(o.toString(), Qna.class);
                    } catch (JsonProcessingException e) {
                        throw ServiceException.from(ExceptionCode.UNEXPECTED_SERVER_ERROR);
                    }
                })
                .map(this::mapToQnaInfo)
                .toList();

        QnaListResponse response = QnaListResponse.builder()
                .qnaInfoList(qnaInfos)
                .build();

        template.convertAndSend(QNA_LIST_DESTINATION + sessionId, response);
    }

    private QnaListResponse.QnaInfo mapToQnaInfo(Qna qna) {
        String nickname;
        if (qna.anonymous()) {
            nickname = "";
        } else {
            nickname = qna.nickname();
        }

        Long likeCount = stringRedisTemplate.opsForSet().size(QNA_LIKE_SET_KEY + qna.qnaId());

        return QnaListResponse.QnaInfo.builder()
                .qnaId(qna.qnaId())
                .userId(qna.userId())
                .nickname(nickname)
                .content(qna.content())
                .likes(likeCount)
                .anonymous(qna.anonymous())
                .build();
    }

    public void addQna(QnaCreateRequest qnaCreateRequest, Long streamId, AuthenticatedUser user) {
        Long qnaId = stringRedisTemplate.opsForValue().increment(QNA_ID_KEY);
        Qna qna = Qna.from(qnaId, qnaCreateRequest, user);

        try {
            String value = objectMapper.writeValueAsString(qna);
            stringRedisTemplate.opsForHash().put(QNA_KEY + streamId, String.valueOf(qna.qnaId()), value);
        } catch (JsonProcessingException e) {
            throw ServiceException.from(ExceptionCode.UNEXPECTED_SERVER_ERROR);
        }

        QnaListResponse.QnaInfo response = this.mapToQnaInfo(qna);

        template.convertAndSend(QNA_DESTINATION + streamId + "/add", response);
        log.info("qna 메시지 전송 - {}", qnaCreateRequest.content());
    }

    public void likeQna(Long streamId, Long qnaId, AuthenticatedUser user) {
        String likeKey = QNA_LIKE_SET_KEY + qnaId;
        String userIdStr = user.userId().toString();

        Boolean isMember = stringRedisTemplate.opsForSet().isMember(likeKey, userIdStr);
        if (Boolean.TRUE.equals(isMember)) {
            throw ServiceException.from(ExceptionCode.ALREADY_LIKED);
        }

        stringRedisTemplate.opsForSet().add(likeKey, userIdStr);

        QnaLikeResponse response = QnaLikeResponse.builder()
                .qnaId(qnaId)
                .likes(stringRedisTemplate.opsForSet().size(likeKey))
                .build();
        template.convertAndSend(QNA_DESTINATION + streamId + "/liked", response);
        log.info("qna 메시지 좋아요 - {}", qnaId);
    }

    public void deleteQna(Long streamId, Long qnaId, AuthenticatedUser user) {
        String qnaKey = QNA_KEY + streamId;
        String qnaField = qnaId.toString();

        Object value = stringRedisTemplate.opsForHash().get(qnaKey, qnaField);
        if (value == null) {
            throw ServiceException.from(ExceptionCode.QNA_NOT_FOUND);
        }

        stringRedisTemplate.opsForHash().delete(qnaKey, qnaField);

        stringRedisTemplate.delete(QNA_LIKE_SET_KEY + qnaId);

        template.convertAndSend(QNA_DESTINATION + streamId + "/delete", qnaId);
        log.info("qna 메시지 삭제 - {}", qnaId);
    }
}