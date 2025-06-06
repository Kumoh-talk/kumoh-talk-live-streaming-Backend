package com.kumoh_talk.streaming.domain.stream.persistent.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.VOD_PATH;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "vod")
public class Vod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vod_id")
    private Long id;

    @Column(length = 30, nullable = false)
    private String title;

    @Column(nullable = false)
    private String camUrl;

    @Column(nullable = false)
    private String slideUrl;

    @Column(nullable = false)
    private LocalTime length;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private Long views;

    @Builder
    public Vod(String title, String camKey, String slideKey, int seconds, String summary) {
        this.title = title;

        this.camUrl = VOD_PATH + "/" + camKey;
        this.slideUrl = VOD_PATH + "/" + slideKey;

        this.length = LocalTime.ofSecondOfDay(seconds);
        this.summary = summary;
        this.views = 0L;
    }
}
