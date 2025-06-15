package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record VodResponse(
    String title,
    String slideUrl,
    String slideTsQuery,
    String camUrl,
    String camTsQuery,
    List<Bookmark> bookmarks
) {
    @Builder
    public record Bookmark(
        Long bookmarkId,
        String title,
        LocalTime time
    ) {
    }
}
