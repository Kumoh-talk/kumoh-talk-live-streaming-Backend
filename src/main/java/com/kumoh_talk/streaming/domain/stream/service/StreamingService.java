package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final String DESKTOP_TYPE = "desktop";
    private static final String WEBCAM_TYPE = "webcam";

    private static final String ALLOWED_STREAM_KEY = "hello";

    public void startStreaming(String name) {
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
        String outputDir = "/tmp";

        // FFmpeg 명령 (HLS + Audio 추출 동시)
        String[] cmd = {
                "ffmpeg", "-i", rtmpUrl,
                "-map", "0:v:0", "-map", "0:a:0",
                "-c:v", "copy", "-c:a", "aac", "-f", "hls",
                "-hls_time", "2", "-hls_list_size", "10", "-hls_flags", "delete_segments",
                outputDir + "/hls/" + name + ".m3u8",

                "-map", "0:a:0", "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", "2", "-hls_list_size", "10", "-hls_flags", "delete_segments",
                outputDir + "/hls_audio/" + name + ".m3u8"
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
}
