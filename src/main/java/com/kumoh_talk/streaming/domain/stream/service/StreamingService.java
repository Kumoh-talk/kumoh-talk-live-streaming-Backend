package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.domain.stream.dto.response.CreateStreamKeyResponse;
import com.kumoh_talk.streaming.domain.stream.dto.response.StreamKeyListResponse;
import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.persistent.repository.VodRepository;
import com.kumoh_talk.streaming.domain.stream.redis.entity.Streaming;
import com.kumoh_talk.streaming.domain.stream.redis.repository.StreamingRedisRepository;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.watchService.HlsWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.*;
import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.SUBSCRIBER_KEY_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final HlsWatcher.ThumbnailEventHandler NOOP_THUMBNAIL_HANDLER = path -> {};

    private static final String STREAMING_ID_KEY = "streaming:id:seq";
    private static final String STREAM_CANDIDATE_KEY = "stream:candidate:keys";
    private static final Duration STREAM_CANDIDATE_KEY_TTL = Duration.ofHours(1);

    private final S3Service s3Service;
    private final VodRepository vodRepository;
    private final StreamingRedisRepository streamingRedisRepository;

    private final StringRedisTemplate stringRedisTemplate;

    private final Map<Path, HlsWatcher> watcherMap = new ConcurrentHashMap<>();

    public void startStreaming(String name, String title) {
        log.info("stream name: {}", name);

        String[] parts = name.split(STREAMING_TYPE_DELIMITER);

        if (!isValidStreamFormat(parts)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_FORMAT);
        }

        String streamKey = parts[0];
        String type = parts[1];

        checkStreamKey(streamKey);

        String streamWatchKey = getOrCreateStreamWatchKey(streamKey, type, title);

        String hlsDir = convertRtmpToHlsWithAudio(name, streamWatchKey, type);

        startWatcher(Path.of(hlsDir), type);
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

    private String getOrCreateStreamWatchKey(String streamKey, String type, String title) {
        Optional<Streaming> savedStreaming = streamingRedisRepository.findByStreamUploadKey(streamKey);
        if (savedStreaming.isPresent()) {
            return getStreamWatchKey(savedStreaming.get(), type);
        }

        Long id = stringRedisTemplate.opsForValue().increment(STREAMING_ID_KEY);
        Streaming streaming = Streaming.builder()
                .id(id)
                .startTime(LocalDateTime.now())
                .title(title)
                .streamUploadKey(streamKey)
                .build();
        Streaming newStreaming = streamingRedisRepository.save(streaming);
        log.info("새로운 스트리밍 생성 - uploadKey:{}, camKey: {}, slideKey: {}", streamKey, newStreaming.getCamWatchKey(), newStreaming.getSlideWatchKey());

        return getStreamWatchKey(newStreaming, type);
    }

    private String getStreamWatchKey(Streaming streaming, String type) {
        if (type.equals(DESKTOP_TYPE)) {
            return streaming.getSlideWatchKey();
        }

        return streaming.getCamWatchKey();
    }

    private String convertRtmpToHlsWithAudio(String streamUploadKey, String streamWatchKey, String type) {
        String rtmpUrl = "rtmp://kumoh-talk-streaming-nginx-rtmp:1935/live/" + streamUploadKey;

        String hlsDir = HLS_OUTPUT_DIR + "/" + streamWatchKey;
        // TODO. ffmpeg 동시 실행되지 않도록 시간차 두기, ffmpeg 실행 리팩토링

        String[] videoCmd = {
                "ffmpeg", "-fflags", "+genpts", "-i", rtmpUrl,
                "-map", "0:v:0", "-map", "0:a:0?",
                "-c:v", "copy", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                "-hls_segment_type", "mpegts",
                hlsDir + "/index.m3u8"
        };

        startFfmpegProcess(videoCmd, hlsDir);

        if (type.equals(WEBCAM_TYPE)) {
            return hlsDir;
        }

        String hlsAudioDir = String.join("/", AUDIO_OUTPUT_DIR, streamWatchKey);

        String[] audioCmd = {
                "ffmpeg", "-fflags", "+genpts", "-i", rtmpUrl,
                "-map", "0:a:0?", "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                "-hls_segment_type", "mpegts",
                hlsAudioDir + "/index.m3u8"
        };

        startFfmpegProcess(audioCmd, hlsAudioDir);

        return hlsDir;
    }

    private void startFfmpegProcess(String[] command, String hlsDir) {
        new File(hlsDir).mkdirs();

        try {
            new ProcessBuilder(command).inheritIO().start();
        } catch (IOException e) {
            log.error("FFmpeg 프로세스 시작 실패({}): {}", hlsDir, e.getMessage());
            throw ServiceException.from(ExceptionCode.FFMPEG_PROCESS_ERROR);
        }
    }

    private void startWatcher(Path dirPath, String type) {
        HlsWatcher.ThumbnailEventHandler thumbnailHandler;
        if (type.equals(DESKTOP_TYPE)) {
            thumbnailHandler = this::extractThumbnail;
        } else {
            thumbnailHandler = NOOP_THUMBNAIL_HANDLER;
        }

        HlsWatcher watcher = HlsWatcher.builder()
                .directoryPath(dirPath)
                .fileEventHandler(s3Service::uploadHlsFile)
                .thumbnailEventHandler(thumbnailHandler)
                .build();

        Thread watcherThread = new Thread(watcher);
        watcherThread.setDaemon(true);
        watcherThread.start();

        watcherMap.put(dirPath, watcher);
    }

    private void extractThumbnail(Path tsFilePath) {
        String inputPath = tsFilePath.toAbsolutePath().toString();
        Path outputPath = tsFilePath.getParent().resolve(THUMBNAIL_NAME);

        String[] thumbnailCmd = {
                "ffmpeg", "-y",
                "-i", inputPath,
                "-sseof", "-0.1",
                "-frames:v", "1",
                "-vf", "scale=" + THUMBNAIL_RESOLUTION,
                "-pix_fmt", "yuv420p",
                outputPath.toString()
        };

        try {
            Process process =  new ProcessBuilder(thumbnailCmd).inheritIO().start();

            if (process.waitFor() == 0) {
                s3Service.uploadHlsFile(outputPath);
                log.info("썸네일 생성 및 업로드 완료: {}", outputPath);
            }
        } catch (IOException | InterruptedException e) {
            log.error("썸네일 생성 중 오류: {}", e.getMessage());
        }
    }

    public void stopStreaming(String name) {
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
            streamWatchKey = streaming.getStreamUploadKey();
        }

        Path hlsDir = Paths.get(HLS_OUTPUT_DIR, streamWatchKey);
        Path hlsAudioDir = Paths.get(AUDIO_OUTPUT_DIR, streamWatchKey);

        watcherMap.get(hlsDir).stopWatching();
        List<String> tsList = s3Service.getFileList(streamWatchKey).stream()
                .filter(path -> path.endsWith(".ts"))
                .sorted(Comparator.comparingInt(this::extractIndex))
                .toList();
        createAndUploadM3U8(streamWatchKey, tsList);

        if (isSlideType) {
            saveVodEntity(streaming, tsList.size() * HLS_TIME);
        }

        try {
            deleteDirectoryRecursively(hlsDir);
            deleteDirectoryRecursively(hlsAudioDir);
            log.info("스트림 폴더 정리 완료: {}", name);
        } catch (IOException e) {
            log.error("폴더 정리 중 오류 발생: {}", name, e);
        }
    }

    private int extractIndex(String tsPath) {
        String filename = tsPath.substring(tsPath.lastIndexOf("/") + 1);
        String numberPart = filename.replaceAll("\\D+", "");
        return Integer.parseInt(numberPart);
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
                .streamKeyList(keySet.stream().toList())
                .build();
    }

    private Long getSubscriberCount(String destination) {
        return stringRedisTemplate.opsForSet().size(SUBSCRIBER_KEY_PREFIX + destination);
    }
}
