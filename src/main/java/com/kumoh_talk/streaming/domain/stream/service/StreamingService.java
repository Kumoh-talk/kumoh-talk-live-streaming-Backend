package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.domain.stream.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.repository.VodRepository;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.watchService.HlsWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kumoh_talk.streaming.global.constant.StreamingConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final String ALLOWED_STREAM_KEY = "hello";
    private static final HlsWatcher.ThumbnailEventHandler NOOP_THUMBNAIL_HANDLER = path -> {};

    private final S3Service s3Service;

    private final VodRepository vodRepository;

    private final Map<Path, HlsWatcher> watcherMap = new ConcurrentHashMap<>();

    public void startStreaming(String name) {
        log.info("stream name: {}", name);

        String[] parts = name.split(STREAMING_TYPE_DELIMITER);

        if (!isValidStreamFormat(parts)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_FORMAT);
        }

        String streamKey = parts[0];
        String type = parts[1];

        checkStreamKey(streamKey);

        convertRtmpToHlsWithAudio(name, type);

        Path hlsDir = Path.of(HLS_OUTPUT_DIR, name);
        startWatcher(hlsDir, type);
    }

    private boolean isValidStreamFormat(String[] parts) {
        if (parts.length != 2) {
            return false;
        }

        return parts[1].equals(DESKTOP_TYPE) || parts[1].equals(WEBCAM_TYPE);
    }


    private void checkStreamKey(String streamKey) {
        // TODO. 저장된 리스트에 존재하는지 확인하는 코드로 변경
        if (!streamKey.equals(ALLOWED_STREAM_KEY)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_KEY);
        }
    }

    private void convertRtmpToHlsWithAudio(String name, String type) {
        String rtmpUrl = "rtmp://nginx-rtmp:1935/live/" + name;

        String hlsDir = HLS_OUTPUT_DIR + "/" + name;

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
            return;
        }

        String hlsAudioDir = String.join("/", AUDIO_OUTPUT_DIR, name);

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
        Path hlsDir = Paths.get(HLS_OUTPUT_DIR, name);
        Path hlsAudioDir = Paths.get(AUDIO_OUTPUT_DIR, name);

        watcherMap.get(hlsDir).stopWatching();
        List<String> tsList = s3Service.getFileList(name).stream()
                .filter(path -> path.endsWith(".ts"))
                .sorted(Comparator.comparingInt(this::extractIndex))
                .toList();
        createAndUploadM3U8(name, tsList);

        String streamKey = name.split(STREAMING_TYPE_DELIMITER)[0];
        String type = name.split(STREAMING_TYPE_DELIMITER)[1];
        if (type.equals(DESKTOP_TYPE)) {
            saveVodEntity(streamKey, tsList.size() * HLS_TIME);
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

    private void saveVodEntity(String streamKey, int seconds) {
        Vod vod = Vod.builder()
                // TODO. streamKey를 통해 조회하여 title, summary 하드코딩 제거
                .title("JPA란 무엇인가")
                .summary("(내용 요약 텍스트 전문이 들어갈 자리)")
                .streamKey(streamKey)
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
}
