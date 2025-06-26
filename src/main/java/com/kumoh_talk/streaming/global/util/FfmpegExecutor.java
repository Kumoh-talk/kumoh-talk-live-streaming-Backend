package com.kumoh_talk.streaming.global.util;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegExecutor {

    private static final String RTMP_URL_PREFIX = "rtmp://kumoh-talk-streaming-nginx-rtmp:1935/live/";

    private final AudioApiClient audioApiClient;
    private final S3Service s3Service;

    public boolean isRtmpStreamReady(String streamUploadKey, int timeoutSeconds) {
        String rtmpUrl = RTMP_URL_PREFIX + streamUploadKey;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-t", "1",
                    "-i", rtmpUrl,
                    "-f", "null",
                    "-"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("RTMP 스트림 준비 확인 실패", e);
            return false;
        }
    }


    public String startVideoFfmpeg(String streamUploadKey, String streamWatchKey) {
        String rtmpUrl = RTMP_URL_PREFIX + streamUploadKey;

        String hlsDir = HLS_OUTPUT_DIR + "/" + streamWatchKey;
        String hlsUrl = String.join("/", HLS_OUTPUT_DIR, streamWatchKey, M3U8_NAME);

        String[] videoCmd = {
                "ffmpeg", "-fflags", "+genpts", "-i", rtmpUrl,
                "-map", "0:v:0", "-map", "0:a:0?",
                "-c:v", "copy", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                "-hls_segment_type", "mpegts",
                hlsUrl
        };

        startFfmpegProcess(videoCmd, hlsDir);

        return hlsDir;
    }

    @Async
    public void startAudioFfmpeg(String streamUploadKey, String streamWatchKey) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.warn("Audio FFmpeg sleep interrupted.");
        }

        String rtmpUrl = RTMP_URL_PREFIX + streamUploadKey;

        String hlsAudioDir = AUDIO_OUTPUT_DIR + "/" + streamWatchKey;
        String hlsAudioUrl = String.join("/", AUDIO_OUTPUT_DIR, streamWatchKey, M3U8_NAME);

        String[] audioCmd = {
                "ffmpeg", "-fflags", "+genpts", "-i", rtmpUrl,
                "-map", "0:a:0?", "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                "-hls_segment_type", "mpegts",
                hlsAudioUrl
        };

        startFfmpegProcess(audioCmd, hlsAudioDir);

        audioApiClient.start(hlsAudioUrl.replace("/tmp/", ""), streamUploadKey); // 우선 업로드 키로 전달
    }

    public void extractThumbnail(Path tsFilePath) {
        String inputPath = tsFilePath.toAbsolutePath().toString();
        Path outputPath = tsFilePath.getParent().resolve(THUMBNAIL_NAME);

        String[] thumbnailCmd = {
                "ffmpeg", "-y",
                "-sseof", "-0.1",
                "-i", inputPath,
                "-frames:v", "1",
                "-vf", "scale=" + THUMBNAIL_RESOLUTION,
                "-color_range", "tv",
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

    private void startFfmpegProcess(String[] command, String hlsDir) {
        new File(hlsDir).mkdirs();

        try {
            new ProcessBuilder(command).inheritIO().start();
        } catch (IOException e) {
            log.error("FFmpeg 프로세스 시작 실패({}): {}", hlsDir, e.getMessage());
            throw ServiceException.from(ExceptionCode.FFMPEG_PROCESS_ERROR);
        }
    }
}

