package com.kumoh_talk.streaming.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatCreateRequestDto(
	@NotBlank
	@Size(max = 200, message = "채팅 글자 수는 200자 이하여야 합니다.")
	String content
) {
}