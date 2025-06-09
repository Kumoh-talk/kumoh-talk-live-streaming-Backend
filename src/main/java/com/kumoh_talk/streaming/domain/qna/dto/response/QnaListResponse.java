package com.kumoh_talk.streaming.domain.qna.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record QnaListResponse(
    List<QnaInfo> qnaInfoList
) {
    @Builder
    public record QnaInfo(
        Long qnaId,
        Long userId,
        String nickname,
        String content,
        Long likes,
        Boolean anonymous
    ) {
    }
}
