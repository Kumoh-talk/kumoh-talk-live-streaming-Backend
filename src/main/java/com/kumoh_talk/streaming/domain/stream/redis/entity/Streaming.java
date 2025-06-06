package com.kumoh_talk.streaming.domain.stream.redis.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@RedisHash(value = "streaming")
public class Streaming {

    @Id
    private Long id;

    private LocalDateTime startTime;

    private String title;

    @Indexed
    private String streamUploadKey;

    private String camWatchKey;

    private String slideWatchKey;

    @Builder
    public Streaming(Long id, LocalDateTime startTime, String title, String streamUploadKey) {
        this.id = id;
        this.startTime = startTime;
        this.title = title;
        this.streamUploadKey = streamUploadKey;
        this.camWatchKey = UUID.randomUUID().toString();
        this.slideWatchKey = UUID.randomUUID().toString();
    }
}