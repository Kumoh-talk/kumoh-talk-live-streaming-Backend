package com.kumoh_talk.streaming.domain.vote.redis.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;
import java.util.stream.IntStream;

@Getter
@NoArgsConstructor
@RedisHash(value = "vote")
public class Vote {

    @Id
    private Long id;

    @Indexed
    private Long streamId;

    private String title;

    private boolean multiple;

    private List<VoteSelect> selects;

    @Builder
    public Vote(Long id, Long streamId, String title, boolean multiple, List<String> selects) {
        this.id = id;
        this.streamId = streamId;
        this.title = title;
        this.multiple = multiple;
        this.selects = IntStream.range(0, selects.size())
                .mapToObj(i -> VoteSelect.builder()
                        .selectId(i)
                        .content(selects.get(i))
                        .build()
                ).toList();
    }

    @Builder
    public record VoteSelect(
        int selectId,
        String content
    ) {
    }
}