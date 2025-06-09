package com.kumoh_talk.streaming.domain.qna.dto.response;

import lombok.Builder;

@Builder
public record QnaLikeResponse(
    Long qnaId,
    Long likes
) {
}
