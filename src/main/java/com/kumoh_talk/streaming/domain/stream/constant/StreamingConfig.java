package com.kumoh_talk.streaming.domain.stream.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StreamingConfig {

    private static String HLS_URL_PREFIX;
    private static String AUDIO_API_URL;

    @Value("${streaming.hls-url-prefix}")
    public void setHlsUrlPrefix(String value) {
        StreamingConfig.HLS_URL_PREFIX = value;
    }

    @Value("${streaming.audio-api-url")
    public void audioApiUrl(String value) {
        StreamingConfig.AUDIO_API_URL = value;
    }

    public static String HLS_URL_PREFIX() {
        return HLS_URL_PREFIX + "hls/";
    }

    public static String HLS_AUDIO_URL_PREFIX() {
        return HLS_URL_PREFIX + "hls_audio/";
    }

    public static String AUDIO_API_URL() {
        return AUDIO_API_URL;
    }
}
