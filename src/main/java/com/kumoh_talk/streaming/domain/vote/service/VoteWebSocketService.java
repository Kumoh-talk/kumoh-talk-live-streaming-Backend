package com.kumoh_talk.streaming.domain.vote.service;

import com.kumoh_talk.streaming.domain.vote.dto.request.CreateVoteRequest;
import com.kumoh_talk.streaming.domain.vote.dto.response.CreateVoteResponse;
import com.kumoh_talk.streaming.domain.vote.redis.entity.Vote;
import com.kumoh_talk.streaming.domain.vote.redis.repository.VoteRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.VOTE_DESTINATION;

@Service
@RequiredArgsConstructor
public class VoteWebSocketService {

    private static final String VOTE_ID_KEY = "vote:id:seq";

    private final VoteRedisRepository voteRedisRepository;
    private final SimpMessagingTemplate template;

    private final StringRedisTemplate stringRedisTemplate;

    public CreateVoteResponse createVote(Long streamId, CreateVoteRequest request) {
        // TODO. 스트리밍이 존재하는지 검증
//        Streaming streaming = streamingRedisRepository.findById(streamId)
//                        .orElseThrow(() -> ServiceException.from(ExceptionCode.STREAMING_NOT_FOUND));

        Long voteId = stringRedisTemplate.opsForValue().increment(VOTE_ID_KEY);
        Vote vote = Vote.builder()
                .id(voteId)
                .streamId(streamId)
                .title(request.title())
                .multiple(request.multiple())
                .selects(request.selects())
                .build();
        voteRedisRepository.save(vote);

        CreateVoteResponse response = CreateVoteResponse.builder()
                .voteId(voteId)
                .title(vote.getTitle())
                .multiple(vote.isMultiple())
                .selects(
                        vote.getSelects().stream()
                                .map(CreateVoteResponse.VoteSelectWithId::new)
                                .toList()
                )
                .build();

        template.convertAndSend(VOTE_DESTINATION + streamId + "/add-vote", response);

        return response;
    }
}
