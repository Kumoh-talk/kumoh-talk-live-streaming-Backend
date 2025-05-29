package com.kumoh_talk.streaming.domain.chat.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.kumoh_talk.streaming.global.config.NoStrip;

public record ChatCreateRequestDto(
		@NotNull
		String nickname,
		@NotNull
		@NoStrip
		@Size(max = 200, message = "채팅 글자 수는 200자 이하여야 합니다.")
		String content
) {
}