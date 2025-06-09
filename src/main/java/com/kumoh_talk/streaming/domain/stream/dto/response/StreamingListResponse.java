package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record StreamingListResponse(
    List<StreamingInfo> streamingList
) {
    @Builder
    public record StreamingInfo(
        Long streamId,
        String title,
        String thumbnailUrl,
        Long viewers
    ) {
    }
}
