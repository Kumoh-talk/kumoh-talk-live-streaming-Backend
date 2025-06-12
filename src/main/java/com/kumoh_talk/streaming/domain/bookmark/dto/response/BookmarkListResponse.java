package com.kumoh_talk.streaming.domain.bookmark.dto.response;

import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record BookmarkListResponse(
    List<BookmarkInfo> bookmarkList
) {
    @Builder
    public record BookmarkInfo(
        Long bookmarkId,
        Long userId,
				Long vodId,
				String title,
				LocalTime time,
    ) {
    }
}