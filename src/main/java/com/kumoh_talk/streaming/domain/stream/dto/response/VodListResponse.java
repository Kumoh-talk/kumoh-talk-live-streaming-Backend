package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record VodListResponse(
    List<VodInfo> vodList
) {
    @Builder
    public record VodInfo(
        Long vodId,
        String thumbnailUrl,
        String title,
        LocalTime length,
        Long views
    ) {
    }
}
