package com.kumoh_talk.streaming.domain.bookmark.persistent.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "bookmark")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "vod_id", nullable = false)
    private Vod vod;

    @Column(length = 20, nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalTime time;

		@Builder
    public Bookmark(Long userId, Long vodId, String title, LocalTime time) {
        this.userId = userId;
        this.vodId = vodId;
        this.title = title;
        this.time = time;
    }
}
