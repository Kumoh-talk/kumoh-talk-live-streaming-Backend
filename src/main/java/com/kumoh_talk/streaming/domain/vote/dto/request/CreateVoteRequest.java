package com.kumoh_talk.streaming.domain.vote.dto.request;

import com.kumoh_talk.streaming.global.valid.EachStringLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateVoteRequest(
    @NotBlank
    @Size(max = 30)
    String title,

    @NotNull
    Boolean multiple,

    @NotNull
    @Size(min = 2, max = 7)
    @EachStringLength(max = 30)
    List<String> selects
) {
}
