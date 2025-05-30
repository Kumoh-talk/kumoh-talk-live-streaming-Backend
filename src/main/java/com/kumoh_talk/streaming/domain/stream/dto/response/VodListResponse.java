package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

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
        Long views
    ) {
    }
}
