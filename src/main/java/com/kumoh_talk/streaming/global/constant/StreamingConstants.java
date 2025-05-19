package com.kumoh_talk.streaming.global.constant;

public final class StreamingConstants {

    public static final String DESKTOP_TYPE = "desktop";
    public static final String WEBCAM_TYPE = "webcam";
    public static final String STREAMING_TYPE_DELIMITER = "_";

    public static final String HLS_OUTPUT_DIR = "/tmp/hls";
    public static final String AUDIO_OUTPUT_DIR = "/tmp/hls_audio";

    public static final Integer HLS_TIME = 1;
    public static final Integer HLS_LIST_SIZE = 3;

    private StreamingConstants() {}
}