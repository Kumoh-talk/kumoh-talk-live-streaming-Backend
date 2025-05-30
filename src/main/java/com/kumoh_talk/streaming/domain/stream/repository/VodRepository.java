package com.kumoh_talk.streaming.domain.stream.repository;

import com.kumoh_talk.streaming.domain.stream.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VodRepository extends JpaRepository<Vod, Long> {
}
