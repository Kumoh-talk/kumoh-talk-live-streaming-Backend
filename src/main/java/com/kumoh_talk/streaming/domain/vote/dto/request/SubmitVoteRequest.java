package com.kumoh_talk.streaming.domain.vote.dto.request;

import java.util.List;

public record SubmitVoteRequest(
    List<Integer> selects
) {
}
