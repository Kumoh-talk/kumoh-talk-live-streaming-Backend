package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final String DESKTOP_TYPE = "desktop";
    private static final String WEBCAM_TYPE = "webcam";

    private static final String OUTPUT_DIR = "/tmp";

    private static final String ALLOWED_STREAM_KEY = "hello";

    public void startStreaming(String name) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error("스레드 대기 실패: {}", e.getMessage());
        }

        log.info("stream name: {}", name);

        String[] parts = name.split("_");

        if (!isValidStreamFormat(parts)) {
            throw ServiceException.from(ExceptionCode.INVALID_STREAM_FORMAT);
        }

        String streamKey = parts[0];

        checkStreamKey(streamKey);

        convertRtmpToHlsWithAudio(name);
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

    private void convertRtmpToHlsWithAudio(String name) {
        String rtmpUrl = "rtmp://nginx-rtmp:1935/live/" + name;

        String hlsDir = OUTPUT_DIR + "/hls/" + name;
        String hlsAudioDir = OUTPUT_DIR + "/hls_audio/" + name;

        new File(hlsDir).mkdirs();
        new File(hlsAudioDir).mkdirs();

        // FFmpeg 명령 (HLS + Audio 추출 동시)
        String[] cmd = {
                "ffmpeg", "-i", rtmpUrl,
                "-map", "0:v:0", "-map", "0:a:0",
                "-c:v", "copy", "-c:a", "aac", "-f", "hls",
                "-hls_time", "1", "-hls_list_size", "6", "-hls_flags", "delete_segments",
                hlsDir + "/index.m3u8",

                "-map", "0:a:0", "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", "1", "-hls_list_size", "6", "-hls_flags", "delete_segments",
                hlsAudioDir + "/index.m3u8"
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        try {
            pb.start();
        } catch (IOException e) {
            log.error("FFmpeg 프로세스 시작 실패", e);
            throw ServiceException.from(ExceptionCode.FFMPEG_PROCESS_ERROR);
        }
    }

    public void stopStreaming(String name) {
        Path hlsDir = Paths.get(OUTPUT_DIR, "hls", name);
        Path hlsAudioDir = Paths.get(OUTPUT_DIR, "hls_audio", name);

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
