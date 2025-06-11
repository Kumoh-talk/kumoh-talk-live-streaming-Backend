package com.kumoh_talk.streaming.domain.vote.controller;

import com.kumoh_talk.streaming.domain.vote.dto.request.CreateVoteRequest;
import com.kumoh_talk.streaming.domain.vote.dto.response.CreateVoteResponse;
import com.kumoh_talk.streaming.domain.vote.service.VoteWebSocketService;
import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteWebSocketService voteWebSocketService;

    // TODO. 추후 ADMIN 계정 받은 후 ROLE_ADMIN만 가능하도록
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping("/{streamId}")
    public ResponseEntity<ResponseBody<CreateVoteResponse>> createVote(
            @PathVariable(name = "streamId") Long streamId,
            @Valid @RequestBody CreateVoteRequest request
    ) {
        CreateVoteResponse response = voteWebSocketService.createVote(streamId, request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
