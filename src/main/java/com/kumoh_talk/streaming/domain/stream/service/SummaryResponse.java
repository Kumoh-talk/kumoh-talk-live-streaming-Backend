package com.kumoh_talk.streaming.domain.stream.service;

import lombok.Builder;

@Builder
public record SummaryResponse(
    String summary
) {
}
