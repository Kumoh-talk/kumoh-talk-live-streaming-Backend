package com.kumoh_talk.streaming.global.exception;

import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.ERROR_DESTINATION;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.ERROR_DESTINATION_PREFIX;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate template;

    @MessageExceptionHandler(ServiceException.class)
    public void handleMyCustomException(ServiceException e, StompHeaderAccessor accessor) {
        template.convertAndSend(ERROR_DESTINATION_PREFIX + accessor.getSessionId() + ERROR_DESTINATION,
                ResponseUtil.createFailureResponse(e.getExceptionCode()));
    }

    @MessageExceptionHandler(Exception.class)
    public void handleAllExceptions(Exception e, StompHeaderAccessor accessor) {
        log.error("Exception Message: {}", e.getMessage());
        template.convertAndSend(ERROR_DESTINATION_PREFIX + accessor.getSessionId() + ERROR_DESTINATION,
                ResponseUtil.createFailureResponse(ExceptionCode.UNEXPECTED_SERVER_ERROR));
    }
}