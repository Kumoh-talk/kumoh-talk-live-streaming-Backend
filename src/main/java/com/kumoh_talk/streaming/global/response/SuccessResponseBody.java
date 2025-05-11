package com.kumoh_talk.streaming.global.response;

import lombok.Getter;

@Getter
public final class SuccessResponseBody<T> extends ResponseBody<T> {
    private final T data;

    SuccessResponseBody() {
        this.success = true;
        this.data = null;
    }

    SuccessResponseBody(T data) {
        this.success = true;
        this.data = data;
    }
}
