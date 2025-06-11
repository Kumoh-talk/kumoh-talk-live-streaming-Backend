package com.kumoh_talk.streaming.domain.vote.redis.repository;

import com.kumoh_talk.streaming.domain.vote.redis.entity.Vote;
import org.springframework.data.repository.CrudRepository;

public interface VoteRedisRepository extends CrudRepository<Vote, Long> {
}
