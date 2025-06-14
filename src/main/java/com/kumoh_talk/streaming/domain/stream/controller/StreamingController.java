package com.kumoh_talk.streaming.domain.stream.controller;

import com.kumoh_talk.streaming.domain.stream.dto.request.CaptionSegmentRequest;
import com.kumoh_talk.streaming.domain.stream.dto.request.ChangeStreamingTitleRequest;
import com.kumoh_talk.streaming.domain.stream.dto.response.*;
import com.kumoh_talk.streaming.domain.stream.service.StreamingService;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping("/streamKey")
    public ResponseEntity<ResponseBody<CreateStreamKeyResponse>> createStreamKey(@AuthenticationPrincipal AuthenticatedUser user) {
        CreateStreamKeyResponse response = streamingService.createStreamKey(user);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    // TODO. 추후 ADMIN 계정 받은 후 ROLE_ADMIN만 가능하도록
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @GetMapping("/streamKey")
    public ResponseEntity<ResponseBody<StreamKeyListResponse>> getStreamKey() {
        StreamKeyListResponse response = streamingService.getStreamKey();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    // TODO. 추후 ADMIN 계정 받은 후 ROLE_ADMIN만 가능하도록
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @PatchMapping("/title")
    public ResponseEntity<ResponseBody<StreamIdResponse>> changeStreamingTitle(@Valid @RequestBody ChangeStreamingTitleRequest request) {
        StreamIdResponse response = streamingService.changeStreamingTitle(request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/list")
    public ResponseEntity<ResponseBody<StreamingListResponse>> getStreamingList() {
        StreamingListResponse response = streamingService.getStreamingList();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/{streamId}")
    public ResponseEntity<ResponseBody<StreamingResponse>> getStreamingInfo(@PathVariable(name = "streamId") Long streamId) {
        StreamingResponse response = streamingService.getStreamingInfo(streamId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @PostMapping("/caption")
    public ResponseEntity<ResponseBody<Void>> postCaption(@RequestBody CaptionSegmentRequest request) {
        streamingService.postCaption(request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

}
