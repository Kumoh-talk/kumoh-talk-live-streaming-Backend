package com.kumoh_talk.streaming.domain.chat.persistent.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @Column(length = 200, nullable = false)
    private String content;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Boolean isBlock;

    @Builder
    public Chat(String content, Long userId, String nickname) {
        this.content = content;
        this.userId = userId;
        this.nickname = nickname;
        this.isBlock = false;
    }
}
