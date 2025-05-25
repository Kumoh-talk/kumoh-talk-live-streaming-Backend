package com.kumoh_talk.streaming.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {
    // common
    UNEXPECTED_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "예상치 못한 서버 에러 발생"),
    BINDING_ERROR(HttpStatus.BAD_REQUEST, "C002", "바인딩시 에러 발생"),
    INVALID_ENDPOINT(HttpStatus.NOT_FOUND, "C003", "잘못된 주소 요청"),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "C004", "잘못된 JSON 데이터 형식"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005","허용하지 않는 method"),

    // video (streaming, vod)
    INVALID_STREAM_FORMAT(HttpStatus.FORBIDDEN, "V001", "잘못된 RTMP 주소 요청"),
    INVALID_STREAM_KEY(HttpStatus.FORBIDDEN, "V002", "허용하지 않는 스트림 키"),
    FFMPEG_PROCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "V003", "FFmpeg 프로세스 실행 실패"),
    DIRECTORY_WATCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "V004", "디렉토리 감시 실패"),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
