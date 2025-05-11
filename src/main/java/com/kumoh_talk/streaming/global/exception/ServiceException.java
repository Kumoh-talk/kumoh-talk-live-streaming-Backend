package com.kumoh_talk.streaming.global.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final ExceptionCode exceptionCode;

    public ServiceException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
    }

    public static ServiceException from(ExceptionCode exceptionCode) {
        return new ServiceException(exceptionCode);
    }
}
