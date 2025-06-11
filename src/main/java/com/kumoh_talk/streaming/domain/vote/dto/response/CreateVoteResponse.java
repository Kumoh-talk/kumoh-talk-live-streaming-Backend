package com.kumoh_talk.streaming.domain.vote.dto.response;

import com.kumoh_talk.streaming.domain.vote.redis.entity.Vote;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateVoteResponse(
    Long voteId,
    String title,
    Boolean multiple,
    List<VoteSelectWithId> selects
) {
    public record VoteSelectWithId(
        Integer selectId,
        String content
    ) {
        public VoteSelectWithId(Vote.VoteSelect select) {
            this(select.selectId(), select.content());
        }
    }
}
