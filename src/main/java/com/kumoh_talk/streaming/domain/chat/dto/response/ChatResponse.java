package com.kumoh_talk.streaming.domain.chat.dto.response;

import lombok.Builder;

@Builder
public record ChatResponse(
    Long userId,
    String nickname,
    Long chatId,
    String content
) {
}
