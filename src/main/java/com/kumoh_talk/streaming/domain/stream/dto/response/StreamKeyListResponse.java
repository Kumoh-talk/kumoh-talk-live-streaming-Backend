package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record StreamKeyListResponse(
    List<String> streamKeyList
) {
}
