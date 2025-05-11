package com.kumoh_talk.streaming.global.response;

import lombok.Getter;

@Getter
public final class FailureResponseBody extends ResponseBody<Void> {
    private final String code;
    private final String message;

    FailureResponseBody(String code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
    }
}
