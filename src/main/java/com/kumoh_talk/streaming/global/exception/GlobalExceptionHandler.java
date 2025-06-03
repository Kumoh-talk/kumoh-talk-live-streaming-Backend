package com.kumoh_talk.streaming.global.exception;

import com.kumoh_talk.streaming.global.response.ResponseBody;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ResponseBody<Void>> businessException(ServiceException e) {
        ExceptionCode exceptionCode = e.getExceptionCode();
        return ResponseEntity.status(exceptionCode.getStatus())
                .body(ResponseUtil.createFailureResponse(exceptionCode));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ResponseBody<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;

        if (isAnonymous) {
            return ResponseEntity
                    .status(ExceptionCode.UN_AUTHENTICATION.getStatus())
                    .body(ResponseUtil.createFailureResponse(ExceptionCode.UN_AUTHENTICATION));
        }

        return ResponseEntity
                .status(ExceptionCode.FORBIDDEN.getStatus())
                .body(ResponseUtil.createFailureResponse(ExceptionCode.FORBIDDEN));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseBody<Void>> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        String customMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(ExceptionCode.BINDING_ERROR.getStatus())
                .body(ResponseUtil.createFailureResponse(ExceptionCode.BINDING_ERROR, customMessage));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseBody<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(ExceptionCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ResponseUtil.createFailureResponse(ExceptionCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseBody<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity
                .status(ExceptionCode.INVALID_ENDPOINT.getStatus())
                .body(ResponseUtil.createFailureResponse(ExceptionCode.INVALID_ENDPOINT));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseBody<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity
                .status(ExceptionCode.INVALID_JSON_FORMAT.getStatus())
                .body(ResponseUtil.createFailureResponse(ExceptionCode.INVALID_JSON_FORMAT));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBody<Void>> exception(Exception e) {
        log.error("Exception Message: {}", e.getMessage());
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.createFailureResponse(ExceptionCode.UNEXPECTED_SERVER_ERROR));
    }
}
