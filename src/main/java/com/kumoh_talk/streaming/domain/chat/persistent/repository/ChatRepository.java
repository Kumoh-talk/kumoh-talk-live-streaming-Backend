package com.kumoh_talk.streaming.domain.chat.persistent.repository;

import com.kumoh_talk.streaming.domain.chat.persistent.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
}
