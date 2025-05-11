package com.kumoh_talk.streaming.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ResponseBody<T> {
    protected Boolean success;
}
