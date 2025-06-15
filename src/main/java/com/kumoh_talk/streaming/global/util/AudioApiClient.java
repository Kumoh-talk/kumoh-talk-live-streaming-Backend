package com.kumoh_talk.streaming.global.util;

import com.kumoh_talk.streaming.domain.stream.constant.StreamingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AudioApiClient {

    private final StreamingConfig streamingConfig;

    private final WebClient webClient;

    public AudioApiClient(StreamingConfig config) {
        this.streamingConfig = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getAudioApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void start(String hlsUrl, String sessionId) {
        Map<String, Object> startPayload = new HashMap<>();
        startPayload.put("hls_url", streamingConfig.getHlsUrlPrefix() + hlsUrl + "/index.m3u8");
        startPayload.put("session_id", sessionId);

        try {
            Map startResponse = webClient.post()
                    .uri("/start")
                    .bodyValue(startPayload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("audio api start request, response: {}", startResponse);
        } catch (WebClientResponseException e) {
            log.error("audio api start error: {}", e.getMessage());
        }
    }

    public void end(String sessionId) {
        Map<String, Object> endPayload = new HashMap<>();
        endPayload.put("session_id", sessionId);

        try {
            Map endResponse = webClient.post()
                    .uri("/end")
                    .bodyValue(endPayload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("audio api end request, response: {}", endResponse);
        } catch (WebClientResponseException e) {
            log.error("audio api end error: {}", e.getMessage());
        }
    }
}
