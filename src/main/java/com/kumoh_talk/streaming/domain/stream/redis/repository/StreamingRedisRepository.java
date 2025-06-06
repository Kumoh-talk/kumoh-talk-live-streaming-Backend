package com.kumoh_talk.streaming.domain.stream.redis.repository;

import com.kumoh_talk.streaming.domain.stream.redis.entity.Streaming;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface StreamingRedisRepository extends CrudRepository<Streaming, Long> {
    Optional<Streaming> findByStreamUploadKey(String streamUploadKey);
}
