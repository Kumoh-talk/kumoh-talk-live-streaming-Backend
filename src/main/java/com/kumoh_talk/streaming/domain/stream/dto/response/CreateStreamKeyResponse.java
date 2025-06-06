package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateStreamKeyResponse(
    String streamKey,
    LocalDateTime expireAt
) {
}
