package com.kumoh_talk.streaming.domain.bookmark.dto.request;

public record CreateBookmarkRequest(
	@NotBlank
	@Size(max = 20, message = "북마크 제목 글자 수는 20자 이하여야 합니다.")
	String title
	LocalTime time
) {
	
}