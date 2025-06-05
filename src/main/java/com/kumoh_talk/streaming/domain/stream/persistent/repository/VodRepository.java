package com.kumoh_talk.streaming.domain.stream.persistent.repository;

import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VodRepository extends JpaRepository<Vod, Long> {
}
