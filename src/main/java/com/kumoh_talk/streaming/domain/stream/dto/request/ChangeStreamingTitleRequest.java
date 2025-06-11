package com.kumoh_talk.streaming.domain.stream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStreamingTitleRequest(
    @NotBlank
    String streamKey,

    @NotBlank
    @Size(max = 30)
    String title
) {
}
