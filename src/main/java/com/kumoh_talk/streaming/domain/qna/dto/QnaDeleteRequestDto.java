package com.kumoh_talk.streaming.domain.qna.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.kumoh_talk.streaming.global.config.NoStrip;

public record QnaDeleteRequestDto(
		@NotNull
		int qnaId
) {
}