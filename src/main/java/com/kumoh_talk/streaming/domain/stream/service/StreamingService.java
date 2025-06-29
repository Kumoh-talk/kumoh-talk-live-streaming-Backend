package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.domain.stream.constant.StreamingConfig;
import com.kumoh_talk.streaming.domain.stream.dto.request.CaptionSegmentRequest;
import com.kumoh_talk.streaming.domain.stream.dto.request.ChangeStreamingTitleRequest;
import com.kumoh_talk.streaming.domain.stream.dto.request.SummaryRequest;
import com.kumoh_talk.streaming.domain.stream.dto.response.*;
import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.persistent.repository.VodRepository;
import com.kumoh_talk.streaming.domain.stream.redis.entity.Streaming;
import com.kumoh_talk.streaming.domain.stream.redis.repository.StreamingRedisRepository;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.util.AudioApiClient;
import com.kumoh_talk.streaming.global.util.FfmpegExecutor;
import com.kumoh_talk.streaming.global.watchService.HlsWatcherRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.*;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final String STREAMING_ID_KEY = "streaming:id:seq";
    private static final String STREAM_CANDIDATE_KEY = "stream:candidate:keys";
    private static final Duration STREAM_CANDIDATE_KEY_TTL = Duration.ofHours(72);
    private static final String STREAM_START_LOCK_PREFIX = "lock:streaming:";

    private final StreamingConfig streamingConfig;
    private final AudioApiClient audioApiClient;
    private final FfmpegExecutor ffmpegExecutor;
    private final HlsWatcherRunner hlsWatcherRunner;

    private final S3Service s3Service;
    private final VodRepository vodRepository;
    private final StreamingRedisRepository streamingRedisRepository;

    private final StringRedisTemplate stringRedisTemplate;

    private final SimpMessagingTemplate template;

    public void startStreaming(String name) {
        log.info("stream name: {}", name);

        String[] parts = name.split(STREAMING_TYPE_DELIMITER);

        if (!isValidStreamFormat(parts)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_FORMAT);
        }

        String streamKey = parts[0];
        String type = parts[1];

        checkStreamKey(streamKey);

        String streamWatchKey = getOrCreateStreamWatchKey(streamKey, type);

        convertRtmpToHlsWithAudio(name, streamWatchKey, type);

        hlsWatcherRunner.startWatcher(streamWatchKey, type);

        log.info("{} 송출 ok", name);
    }

    private boolean isValidStreamFormat(String[] parts) {
        if (parts.length != 2) {
            return false;
        }

        return parts[1].equals(DESKTOP_TYPE) || parts[1].equals(WEBCAM_TYPE);
    }


    private void checkStreamKey(String streamKey) {
        Boolean isValidKey = stringRedisTemplate.hasKey(STREAM_CANDIDATE_KEY + ":" + streamKey);

        if (isValidKey == null) {
            throw ServiceException.from(ExceptionCode.UNEXPECTED_SERVER_ERROR);
        }

        if (!isValidKey) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_KEY);
        }
    }

    private String getOrCreateStreamWatchKey(String streamKey, String type) {
        String lockKey = STREAM_START_LOCK_PREFIX + streamKey;

        boolean lockAcquired = Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofMillis(500))
        );

        if (!lockAcquired) { // 다른 스레드가 락을 얻고 있는 경우
            Optional<Streaming> savedStreaming = streamingRedisRepository.findByStreamUploadKey(streamKey);
            if (savedStreaming.isPresent()) {
                return getStreamWatchKey(savedStreaming.get(), type);
            }
            // 락은 있지만 redis 저장은 아직 안 된 경우
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}

            Streaming streaming = streamingRedisRepository.findByStreamUploadKey(streamKey)
                    .orElseThrow(() -> new IllegalStateException("스트리밍 정보 동기화 실패"));

            return getStreamWatchKey(streaming, type);
        }

        try {
            Optional<Streaming> savedStreaming = streamingRedisRepository.findByStreamUploadKey(streamKey);
            if (savedStreaming.isPresent()) {
                return getStreamWatchKey(savedStreaming.get(), type);
            }

            Long id = stringRedisTemplate.opsForValue().increment(STREAMING_ID_KEY);
            Streaming streaming = Streaming.builder()
                    .id(id)
                    .startTime(LocalDateTime.now())
                    .title("")
                    .streamUploadKey(streamKey)
                    .build();
            Streaming newStreaming = streamingRedisRepository.save(streaming);
            log.info("새로운 스트리밍 생성 - uploadKey: {}, camKey: {}, slideKey: {}", streamKey, newStreaming.getCamWatchKey(), newStreaming.getSlideWatchKey());

            return getStreamWatchKey(newStreaming, type);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private String getStreamWatchKey(Streaming streaming, String type) {
        if (type.equals(DESKTOP_TYPE)) {
            return streaming.getSlideWatchKey();
        }

        return streaming.getCamWatchKey();
    }

    private void convertRtmpToHlsWithAudio(String streamUploadKey, String streamWatchKey, String type) {
        if (type.equals(DESKTOP_TYPE)) {
            ffmpegExecutor.startAudioFfmpeg(streamUploadKey, streamWatchKey);
        }
    }

    public void stopStreaming(String name) {
        log.info("end stream: {}", name);

        String[] parts = name.split(STREAMING_TYPE_DELIMITER);
        String streamKey = parts[0];
        String type = parts[1];

        Streaming streaming = streamingRedisRepository.findByStreamUploadKey(streamKey)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.INVALID_STREAM_KEY));

        boolean isSlideType = type.equals(DESKTOP_TYPE);
        String streamWatchKey;
        if (isSlideType) {
            streamWatchKey = streaming.getSlideWatchKey();
        } else {
            streamWatchKey = streaming.getCamWatchKey();
        }

        // TODO. VOD에서 5초 정도 잘리는 문제(hls가 생성되고 업로드 되기 전에 m3u8 파일 생성) 해결
        List<String> tsList = s3Service.getFileList(streamWatchKey).stream()
                .filter(path -> path.endsWith(".ts"))
                .sorted(Comparator.comparingInt(this::extractIndex))
                .toList();
        createAndUploadM3U8(streamWatchKey, tsList);


        if (isSlideType) {
            saveVodEntity(streaming, tsList.size() * HLS_TIME);
            audioApiClient.end(streamKey);  // 우선 업로드 키로 전달
        }

        Long result = stringRedisTemplate.opsForValue().increment("stream:done:count:" + streamKey);

        if (result != null && result == 2) {
            streamingRedisRepository.delete(streaming);
            // TODO. 레디스에 남은 qna, vote 정리
        }

        Path hlsDir = Paths.get(HLS_OUTPUT_DIR, streamWatchKey);
        try {
            deleteDirectoryRecursively(hlsDir);

            if (isSlideType) {
                return;
            }

            Path hlsAudioDir = Paths.get(AUDIO_OUTPUT_DIR, streamWatchKey);
            deleteDirectoryRecursively(hlsAudioDir);

            log.info("스트림 폴더 정리 완료: {}", hlsDir);
        } catch (IOException e) {
            log.error("폴더 정리 중 오류 발생: {}", hlsDir, e);
        }
    }

    private int extractIndex(String tsPath) {
        String filename = tsPath.substring(tsPath.lastIndexOf("/") + 1);

        int dashIndex = filename.lastIndexOf('-');
        int dotIndex = filename.lastIndexOf('.');
        String numberPart = filename.substring(dashIndex + 1, dotIndex);

        return Integer.parseInt(numberPart); // 이 부분은 int 범위만 가능
    }

    private void saveVodEntity(Streaming streaming, int seconds) {
        Vod vod = Vod.builder()
                .title(streaming.getTitle())
                .summary(streaming.getSummary())
                .camKey(streaming.getCamWatchKey())
                .slideKey(streaming.getSlideWatchKey())
                .seconds(seconds)
                .build();

        vodRepository.save(vod);
    }

    private void createAndUploadM3U8(String name, List<String> tsList) {
        StringBuilder m3u8 = new StringBuilder();
        m3u8.append("#EXTM3U\n");
        m3u8.append("#EXT-X-VERSION:6\n");
        m3u8.append(String.format("#EXT-X-TARGETDURATION:%d\n", HLS_TIME));
        m3u8.append("#EXT-X-MEDIA-SEQUENCE:0\n");

        for (String ts : tsList) {
            m3u8.append(String.format("#EXTINF:%.3f,\n", HLS_TIME.doubleValue()));
            m3u8.append(ts.substring(ts.lastIndexOf("/") + 1)).append("\n");
        }

        m3u8.append("#EXT-X-ENDLIST\n");

        s3Service.uploadM3U8File(name, m3u8.toString().getBytes());
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public CreateStreamKeyResponse createStreamKey(AuthenticatedUser user) {
        CreateStreamKeyResponse response = CreateStreamKeyResponse.builder()
                .streamKey(UUID.randomUUID().toString())
                .expireAt(LocalDateTime.now().plus(STREAM_CANDIDATE_KEY_TTL))
                .build();

        stringRedisTemplate.opsForValue().set(STREAM_CANDIDATE_KEY + ":" + response.streamKey(),
                user.userId().toString(),
                STREAM_CANDIDATE_KEY_TTL);

        return response;
    }

    public StreamKeyListResponse getStreamKey() {
        String pattern = STREAM_CANDIDATE_KEY + ":*";

        Set<String> keySet = stringRedisTemplate.keys(pattern);
        if (keySet == null) {
            return StreamKeyListResponse.builder()
                    .streamKeyList(List.of())
                    .build();
        }

        return StreamKeyListResponse.builder()
                .streamKeyList(
                        keySet.stream()
                                .map(s -> s.replaceFirst(STREAM_CANDIDATE_KEY + ":", ""))
                                .toList()
                )
                .build();
    }

    @Transactional
    public StreamIdResponse changeStreamingTitle(ChangeStreamingTitleRequest request) {
        Streaming streaming = streamingRedisRepository.findByStreamUploadKey(request.streamKey())
                .orElseThrow(() -> ServiceException.from(ExceptionCode.STREAMING_NOT_FOUND));

        streaming.setTitle(request.title());
        streamingRedisRepository.save(streaming);

        Map<String, String> response = Map.of("title", streaming.getTitle());

        template.convertAndSend(TITLE_DESTINATION + streaming.getId(), response);

        return StreamIdResponse.builder()
                .streamId(streaming.getId())
                .build();
    }

    public StreamingListResponse getStreamingList() {
        Stream<Streaming> streamingStream =
                StreamSupport.stream(streamingRedisRepository.findAll().spliterator(), false);

        List<StreamingListResponse.StreamingInfo> streamingList = streamingStream
                .map(streaming -> StreamingListResponse.StreamingInfo.builder()
                        .streamId(streaming.getId())
                        .title(streaming.getTitle())
                        .thumbnailUrl(s3Service.generateThumbnailUrl(streaming.getSlideWatchKey()))
                        .viewers(getSubscriberCount(streaming.getId().toString()))
                        .build()
                ).toList();

        return StreamingListResponse.builder()
                .streamingList(streamingList)
                .build();
    }

    private Long getSubscriberCount(String streamId) {
        return stringRedisTemplate.opsForSet().size(SUBSCRIBER_KEY_PREFIX + streamId);
    }

    public StreamingResponse getStreamingInfo(Long streamId) {
        Streaming streaming = streamingRedisRepository.findById(streamId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.STREAMING_NOT_FOUND));

        return StreamingResponse.builder()
                .streamId(streamId)
                .title(streaming.getTitle())
                .camUrl(streamingConfig.getHlsUrlPrefix() + streaming.getCamWatchKey() + ".m3u8")
                .slideUrl(streamingConfig.getHlsUrlPrefix() + streaming.getSlideWatchKey() + ".m3u8")
                .summary(streaming.getSummary())
                .build();
    }

    public void postCaption(CaptionSegmentRequest request) {
        CaptionSegmentResponse response = CaptionSegmentResponse.builder()
                .duration(request.end() - request.start())
                .text(request.text())
                .build();

        log.info("자막 생성: {}", request.text());
        template.convertAndSend(CAPTION_DESTINATION, response);

        // TODO. vtt 파일 생성
    }

    public void postSummary(SummaryRequest request) {
        Streaming streaming = streamingRedisRepository.findByStreamUploadKey(request.session_id())
                .orElseThrow(() -> ServiceException.from(ExceptionCode.STREAMING_NOT_FOUND));

        streaming.setTitle(streaming.getTitle() + request.summary());
        streamingRedisRepository.save(streaming);

        SummaryResponse response = SummaryResponse.builder()
                .summary(streaming.getSummary())
                .build();

        template.convertAndSend(SUMMARY_DESTINATION, response);
    }
}
