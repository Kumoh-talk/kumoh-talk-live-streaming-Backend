package com.kumoh_talk.streaming.global.socket.constant;

public final class WebSocketConstants {

    public static final String STOMP_ENDPOINT = "/ws-stomp";

    public static final String CHAT_DESTINATION = "/streaming/chat/";
    public static final String QNA_DESTINATION = "/streaming/qna/";
    public static final String QNA_LIST_DESTINATION = "/streaming/qna-list/";
    public static final String VOTE_DESTINATION = "/streaming/vote/";
    public static final String VOTE_LIST_DESTINATION = "/streaming/vote-list/";
    public static final String CAPTION_DESTINATION = "/streaming/caption";
    public static final String SUMMARY_DESTINATION = "/streaming/summary";

    public static final String ERROR_DESTINATION = "/queue/errors";
    public static final String ERROR_DESTINATION_PREFIX = "/user/";

    public static final String SUBSCRIBER_KEY_PREFIX = "sub:"; // sub:<streamId> → Set<sessionId>
    public static final String SESSION_KEY_PREFIX = "session:"; // session:<sessionId> → Set<streamId>

    private WebSocketConstants() {}
}
