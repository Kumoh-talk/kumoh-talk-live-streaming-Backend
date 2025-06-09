package com.kumoh_talk.streaming.global.socket.constant;

public final class WebSocketConstants {

    public static final String STOMP_ENDPOINT = "/ws-stomp";

    public static final String CHAT_DESTINATION = "/streaming/chat/";
    public static final String QNA_DESTINATION = "/streaming/qna/";
    public static final String QNA_LIST_DESTINATION = "/streaming/qna-list/";

    public static final String ERROR_DESTINATION = "/queue/errors";
    public static final String ERROR_DESTINATION_PREFIX = "/user/";

    private WebSocketConstants() {}
}
