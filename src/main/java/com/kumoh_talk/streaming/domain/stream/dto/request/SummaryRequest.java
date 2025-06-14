package com.kumoh_talk.streaming.domain.stream.dto.request;

public record SummaryRequest(
    String session_id,
    String summary
) {
}
