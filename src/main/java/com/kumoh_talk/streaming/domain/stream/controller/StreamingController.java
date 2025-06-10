package com.kumoh_talk.streaming.domain.stream.controller;

import com.kumoh_talk.streaming.domain.stream.dto.response.CreateStreamKeyResponse;
import com.kumoh_talk.streaming.domain.stream.dto.response.StreamKeyListResponse;
import com.kumoh_talk.streaming.domain.stream.dto.response.StreamingListResponse;
import com.kumoh_talk.streaming.domain.stream.service.StreamingService;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    // TODO. 추후 ADMIN 계정 받은 후 ROLE_ADMIN만 가능하도록
    @PreAuthorize("hasAnyRole(ROLE_USER, ROLE_ADMIN)")
    @PostMapping("/streamKey")
    public ResponseEntity<ResponseBody<CreateStreamKeyResponse>> createStreamKey(@AuthenticationPrincipal AuthenticatedUser user) {
        CreateStreamKeyResponse response = streamingService.createStreamKey(user);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    // TODO. 추후 ADMIN 계정 받은 후 ROLE_ADMIN만 가능하도록
    @PreAuthorize("hasAnyRole(ROLE_USER, ROLE_ADMIN)")
    @GetMapping("/streamKey")
    public ResponseEntity<ResponseBody<StreamKeyListResponse>> getStreamKey() {
        StreamKeyListResponse response = streamingService.getStreamKey();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/list")
    public ResponseEntity<ResponseBody<StreamingListResponse>> getStreamingList() {
        StreamingListResponse response = streamingService.getStreamingList();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
