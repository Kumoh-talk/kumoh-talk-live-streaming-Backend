package com.kumoh_talk.streaming.domain.bookmark.controller;

import com.kumoh_talk.streaming.domain.bookmark.dto.response.BookmarkListResponse;
import com.kumoh_talk.streaming.domain.bookmark.dto.request.CreateBookmarkRequest;
import com.kumoh_talk.streaming.domain.bookmark.service.BookmarkService;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookmark")
@RequiredArgsConstructor
public class BookmarkController {
	private final BookmarkService bookmarkService;

	@PreAuthorize("hasAnyRole('ROLE_USER')")
	@GetMapping("/{vodId}")
	public ResponseEntity<ResponseBody<BookmarkListResponse>> getBookmarkList(
						@AuthenticationPrincipal CustomUserDetails user,
						@PathVariable Long vodId,
	) {
		BookmarkListResponse response = bookmarkService.getBookmarkList(user.userId(), vodId);
		return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
	}

	@PreAuthorize("hasAnyRole('ROLE_USER')")
    @PostMapping("/{vodId}")
    public ResponseEntity<ResponseBody<Void>> postBookmark(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long vodId,
            @RequestBody CreateBookmarkRequest req
    ) {
        bookmarkService.createBookmark(user.userId(), vodId, req);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(null));
    }

    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @DeleteMapping("/{vodId}/{bookmarkId}")
    public ResponseEntity<ResponseBody<Void>> deleteBookmark(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long vodId,
            @PathVariable Long bookmarkId
    ) {
        bookmarkService.deleteBookmark(user.userId(), vodId, bookmarkId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(null));
    }
}