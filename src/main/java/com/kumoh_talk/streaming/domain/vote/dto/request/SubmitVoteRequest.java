package com.kumoh_talk.streaming.domain.vote.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitVoteRequest(
    @NotNull
    @Size(min = 1)
    List<Integer> selects
) {
}
