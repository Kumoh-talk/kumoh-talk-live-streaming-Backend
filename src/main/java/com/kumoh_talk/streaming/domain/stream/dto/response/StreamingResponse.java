package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

@Builder
public record StreamingResponse(
    Long streamId,
    String title,
    String camUrl,
    String slideUrl
) {
}
