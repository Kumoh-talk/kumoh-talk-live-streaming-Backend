package com.kumoh_talk.streaming.domain.stream.controller;

import com.kumoh_talk.streaming.domain.stream.dto.response.CreateStreamKeyResponse;
import com.kumoh_talk.streaming.domain.stream.service.StreamingService;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamingController {
    private final StreamingService streamingService;

    @PostMapping("/start")
    public ResponseEntity<ResponseBody<Void>> startStream(@RequestParam("name") String name) {
        streamingService.startStreaming(name);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    @PostMapping("/stop")
    public ResponseEntity<ResponseBody<Void>> stopStream(@RequestParam("name") String name) {
        streamingService.stopStreaming(name);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    @PreAuthorize("hasAnyRole(ROLE_ADMIN)")
    @PostMapping("/create-streamKey")
    public ResponseEntity<ResponseBody<CreateStreamKeyResponse>> createStreamKey(@AuthenticationPrincipal AuthenticatedUser user) {
        CreateStreamKeyResponse response = streamingService.createStreamKey(user);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
