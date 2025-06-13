package com.kumoh_talk.streaming.domain.vote.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record VoteResultResponse(
    Long voteId,
    List<VoteCount> voteCounts
) {
    @Builder
    public record VoteCount(
        Integer selectId,
        Long count
    ) {
    }
}
