package com.kumoh_talk.streaming.domain.vote.redis.repository;

import com.kumoh_talk.streaming.domain.vote.redis.entity.Vote;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VoteRedisRepository extends CrudRepository<Vote, Long> {
    List<Vote> findByStreamId(Long streamId);
}
