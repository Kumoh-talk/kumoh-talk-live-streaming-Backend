package com.kumoh_talk.streaming.domain.stream.controller;

import com.kumoh_talk.streaming.domain.stream.dto.response.VodListResponse;
import com.kumoh_talk.streaming.domain.stream.dto.response.VodResponse;
import com.kumoh_talk.streaming.domain.stream.service.VodService;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vod")
@RequiredArgsConstructor
public class VodController {
    private final VodService vodService;

    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping
    public ResponseEntity<ResponseBody<VodListResponse>> getVodList() {
        VodListResponse vodListResponse = vodService.getVodList();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(vodListResponse));
    }

    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/{vodId}")
    public ResponseEntity<ResponseBody<VodResponse>> getVod(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long vodId
    ) {
        VodResponse vodResponse = vodService.getVod(vodId, user.userId());
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(vodResponse));
    }
}
