package com.kumoh_talk.streaming.domain.stream.service;

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

import static com.kumoh_talk.streaming.global.constant.StreamingConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final S3Service s3Service;

    private static final String ALLOWED_STREAM_KEY = "hello";

    public void startStreaming(String name) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error("스레드 대기 실패: {}", e.getMessage());
        }

        log.info("stream name: {}", name);

        String[] parts = name.split(STREAMING_TYPE_DELIMITER);

        if (!isValidStreamFormat(parts)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_FORMAT);
        }

        String streamKey = parts[0];

        checkStreamKey(streamKey);

        convertRtmpToHlsWithAudio(name, parts[1]);
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
                "ffmpeg", "-i", rtmpUrl,
                "-map", "0:v:0", "-map", "0:a:0",
                "-c:v", "copy", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                hlsDir + "/index.m3u8"
        };

        startFfmpegProcess(videoCmd, hlsDir);
        startWatcher(hlsDir);

        if (type.equals(WEBCAM_TYPE)) {
            return;
        }

        String hlsAudioDir = String.join("/", AUDIO_OUTPUT_DIR, name);

        String[] audioCmd = {
                "ffmpeg", "-i", rtmpUrl,
                "-map", "0:a:0", "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
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

    private void startWatcher(String dirPath) {
        HlsWatcher.FileEventHandler handler = filePath -> {
            log.info("새 파일 감지됨: {}", filePath);
            s3Service.uploadHlsFile(filePath);
        };

        HlsWatcher watcher = HlsWatcher.builder()
                .directoryPath(dirPath)
                .fileEventHandler(handler)
                .build();

        Thread watcherThread = new Thread(watcher);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public void stopStreaming(String name) {
        Path hlsDir = Paths.get(HLS_OUTPUT_DIR, name);
        Path hlsAudioDir = Paths.get(AUDIO_OUTPUT_DIR, name);

        // TODO. 디렉토리 감시 종료 및 데이터베이스 저장

        try {
            deleteDirectoryRecursively(hlsDir);
            deleteDirectoryRecursively(hlsAudioDir);
            log.info("스트림 폴더 정리 완료: {}", name);
        } catch (IOException e) {
            log.error("폴더 정리 중 오류 발생: {}", name, e);
        }
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
