package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

@Builder
public record SummaryResponse(
    String summary
) {
}
