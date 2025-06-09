package com.kumoh_talk.streaming.domain.qna.redis.entity;

import com.kumoh_talk.streaming.domain.qna.dto.request.QnaCreateRequest;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import lombok.Builder;

@Builder
public record Qna(
    Long qnaId,
    Long userId,
    String nickname,
    String content,
    Boolean anonymous
) {
    public static Qna from(Long qnaId, QnaCreateRequest qnaCreateRequest, AuthenticatedUser user) {
        return Qna.builder()
                .qnaId(qnaId)
                .userId(user.userId())
                .nickname(user.nickname())
                .content(qnaCreateRequest.content())
                .anonymous(qnaCreateRequest.anonymous())
                .build();
    }
}
