package com.kumoh_talk.streaming.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaCreateRequest(
	@NotBlank
	@Size(max = 200, message = "Q&A 글자 수는 200자 이하여야 합니다.")
	String content,

	boolean anonymous
) {
}