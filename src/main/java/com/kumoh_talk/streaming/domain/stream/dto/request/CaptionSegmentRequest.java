package com.kumoh_talk.streaming.domain.stream.dto.request;

public record CaptionSegmentRequest(
    double start,
    double end,
    String text
) {
}