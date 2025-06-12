package com.kumoh_talk.streaming.domain.vote.controller;

import com.kumoh_talk.streaming.domain.vote.dto.request.SubmitVoteRequest;
import com.kumoh_talk.streaming.domain.vote.service.VoteWebSocketService;
import com.kumoh_talk.streaming.global.auth.constant.Role;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.socket.security.WebSocketAuthValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class VoteWebSocketController {

    private final WebSocketAuthValidator webSocketAuthValidator;
    private final VoteWebSocketService voteWebSocketService;

    @MessageMapping("/streaming/{streamId}/vote-list")
    public void getVoteList(
            @DestinationVariable Long streamId,
            StompHeaderAccessor headerAccessor
    ) {
        voteWebSocketService.getVoteList(streamId, headerAccessor.getSessionId());
    }

    @MessageMapping("/streaming/{streamId}/submit-vote/{voteId}")
    public void submitVoteList(
            @Payload @Valid SubmitVoteRequest request,
            @DestinationVariable Long streamId,
            @DestinationVariable Long voteId,
            StompHeaderAccessor headerAccessor
    ) {
        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) webSocketAuthValidator.validateTokenAndRole(headerAccessor, Role.ROLE_USER.name());
        voteWebSocketService.submitVote(streamId, voteId, request, authenticatedUser);
    }
}
