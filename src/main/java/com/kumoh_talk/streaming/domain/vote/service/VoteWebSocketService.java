package com.kumoh_talk.streaming.domain.vote.service;

import com.kumoh_talk.streaming.domain.vote.dto.request.CreateVoteRequest;
import com.kumoh_talk.streaming.domain.vote.dto.request.SubmitVoteRequest;
import com.kumoh_talk.streaming.domain.vote.dto.response.CreateVoteResponse;
import com.kumoh_talk.streaming.domain.vote.dto.response.VoteListResponse;
import com.kumoh_talk.streaming.domain.vote.dto.response.VoteResultResponse;
import com.kumoh_talk.streaming.domain.vote.redis.entity.Vote;
import com.kumoh_talk.streaming.domain.vote.redis.repository.VoteRedisRepository;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.VOTE_DESTINATION;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.VOTE_LIST_DESTINATION;

@Service
@RequiredArgsConstructor
public class VoteWebSocketService {

    private static final String VOTE_ID_KEY = "vote:id:seq";

    private static final String SELECT_KEY_PREFIX = "vote:select:"; // select:<selectId> → Set<userId>
    private static final String USER_KEY_PREFIX = "vote:user:"; // user:<userId> → Set<selectId>

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

    public void getVoteList(Long streamId, String sessionId) {
        List<Vote> votes = voteRedisRepository.findByStreamId(streamId);

        List<VoteListResponse.VoteInfo> voteInfos = votes.stream()
                .map(this::mapToVoteInfo)
                .toList();

        VoteListResponse response = VoteListResponse.builder()
                .voteInfoList(voteInfos)
                .build();

        template.convertAndSend(VOTE_LIST_DESTINATION + sessionId, response);
    }

    private VoteListResponse.VoteInfo mapToVoteInfo(Vote vote) {
        return VoteListResponse.VoteInfo.builder()
                .voteId(vote.getId())
                .title(vote.getTitle())
                .multiple(vote.isMultiple())
                .selects(
                        vote.getSelects().stream()
                                .map(VoteListResponse.VoteInfo.VoteSelectWithId::new)
                                .toList()
                )
                .build();
    }

    public void submitVote(Long streamId, Long voteId, SubmitVoteRequest request, AuthenticatedUser user) {
        Vote vote = voteRedisRepository.findById(voteId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOTE_NOT_FOUND));

        if (!streamId.equals(vote.getStreamId())) {
            throw ServiceException.from(ExceptionCode.VOTE_NOT_FOUND);
        }

        if (!vote.isMultiple() && request.selects().size() > 1) {
            throw ServiceException.from(ExceptionCode.MULTIPLE_SELECT_NOT_ALLOWED);
        }

        if (!request.selects().stream().allMatch(num -> num >= 0 && num <= vote.getSelects().size())) {
            throw ServiceException.from(ExceptionCode.INVALID_SELECT);
        }

        request.selects()
                .forEach(s ->
                        stringRedisTemplate.opsForSet().add(SELECT_KEY_PREFIX + voteId + ":" + s,
                                user.userId().toString())
                );

        stringRedisTemplate.opsForSet().add(USER_KEY_PREFIX + voteId + ":" + user.userId(),
                request.selects().stream()
                        .map(String::valueOf)
                        .toArray(String[]::new)
        );
    }

    public VoteResultResponse getVoteResult(Long streamId, Long voteId) {
        Vote vote = voteRedisRepository.findById(voteId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOTE_NOT_FOUND));

        if (!streamId.equals(vote.getStreamId())) {
            throw ServiceException.from(ExceptionCode.VOTE_NOT_FOUND);
        }

        List<VoteResultResponse.VoteCount> voteCounts = vote.getSelects().stream()
                .map(Vote.VoteSelect::selectId)
                .map(s -> VoteResultResponse.VoteCount.builder()
                        .selectId(s)
                        .count(stringRedisTemplate.opsForSet().size(SELECT_KEY_PREFIX + voteId + ":" + s))
                        .build())
                .toList();

        return VoteResultResponse.builder()
                .voteId(vote.getId())
                .voteCounts(voteCounts)
                .build();
    }

    public VoteResultResponse closeVote(Long streamId, Long voteId) {
        Vote vote = voteRedisRepository.findById(voteId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOTE_NOT_FOUND));

        if (!streamId.equals(vote.getStreamId())) {
            throw ServiceException.from(ExceptionCode.VOTE_NOT_FOUND);
        }

        List<VoteResultResponse.VoteCount> voteCounts = vote.getSelects().stream()
                .map(Vote.VoteSelect::selectId)
                .map(s -> VoteResultResponse.VoteCount.builder()
                        .selectId(s)
                        .count(stringRedisTemplate.opsForSet().size(SELECT_KEY_PREFIX + voteId + ":" + s))
                        .build())
                .toList();

        VoteResultResponse response = VoteResultResponse.builder()
                .voteId(vote.getId())
                .voteCounts(voteCounts)
                .build();

        this.deleteVote(vote);

        template.convertAndSend(VOTE_DESTINATION + streamId + "/close", response);

        return response;
    }

    private void deleteVote(Vote vote) {
        ScanOptions options = ScanOptions.scanOptions()
                .match("vote:*:" + vote.getId() + "*")
                .count(1000)
                .build();

        RedisConnection connection = stringRedisTemplate.getRequiredConnectionFactory().getConnection();

        Cursor<byte[]> cursor = ((RedisKeyCommands) connection).scan(options);

        List<String> keysToDelete = new ArrayList<>();
        while (cursor.hasNext()) {
            keysToDelete.add(new String(cursor.next()));
        }

        if (!keysToDelete.isEmpty()) {
            stringRedisTemplate.delete(keysToDelete);
        }

        voteRedisRepository.delete(vote);
    }
}
