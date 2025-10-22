package com.kumoh_talk.streaming.domain.stream.dto.response;

import lombok.Builder;

@Builder
public record VodResponse(
    String title,
    String slideUrl,
    String slideTsQuery,
    String camUrl,
    String camTsQuery
) {
}
