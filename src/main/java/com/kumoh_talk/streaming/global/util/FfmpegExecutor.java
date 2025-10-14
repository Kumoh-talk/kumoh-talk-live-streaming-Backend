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

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegExecutor {

    private static final String RTMP_URL_PREFIX = "rtmp://kumoh-talk-streaming-nginx-rtmp:1935/live/";

    private final AudioApiClient audioApiClient;
    private final S3Service s3Service;

    public String startVideoFfmpeg(String streamUploadKey, String streamWatchKey) {
        String rtmpUrl = RTMP_URL_PREFIX + streamUploadKey;

        // ex) hlsDir: /tmp/hls/abcd1234, hlsUrl: /tmp/hls/abcd1234/index.m3u8
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

        // ex) hlsUrl: /tmp/hls/abcd1234/index.m3u8, hlsAudioDir: /tmp/hls_audio/abcd1234, hlsAudioUrl:
        String hlsUrl = String.join("/",HLS_OUTPUT_DIR, streamWatchKey, M3U8_NAME);

        String hlsAudioDir = AUDIO_OUTPUT_DIR + "/" + streamWatchKey;
        String hlsAudioUrl = String.join("/", AUDIO_OUTPUT_DIR, streamWatchKey, M3U8_NAME);

        String[] audioCmd = {
                "ffmpeg", "-i", hlsUrl,
                "-vn", "-c:a", "aac", "-f", "hls",
                "-hls_time", HLS_TIME.toString(),
                "-hls_list_size", HLS_LIST_SIZE.toString(),
                "-hls_flags", "delete_segments",
                "-hls_segment_type", "mpegts",
                hlsAudioUrl
        };

        new File(hlsAudioDir).mkdirs();

        try {
            new ProcessBuilder(audioCmd).inheritIO().start();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.warn("Audio FFmpeg sleep interrupted.");
            }

            audioApiClient.start(hlsAudioUrl.replace("/tmp/", ""), streamUploadKey.split(STREAMING_TYPE_DELIMITER)[0]);

        } catch (IOException e) {
            log.error("오디오 분리 실패({}): {}", hlsAudioDir, e.getMessage());
            throw ServiceException.from(ExceptionCode.FFMPEG_PROCESS_ERROR);
        }
    }

    public void extractThumbnail(Path tsFilePath, String streamWatchKey) {
        String inputPath = tsFilePath.toAbsolutePath().toString();
        String filename = streamWatchKey + "_" + THUMBNAIL_NAME;
        Path outputPath = Path.of(THUMBNAIL_DIR, filename);

        // TODO. 썸네일 생성 이슈
        String[] thumbnailCmd = {
                "ffmpeg", "-y",
                "-sseof", "-0.5",
                "-i", inputPath,
                "-frames:v", "1",
                "-vf", "scale=" + THUMBNAIL_RESOLUTION,
                "-color_range", "tv",
                "-update", "1",
                outputPath.toString()
        };

        try {
            Process process = new ProcessBuilder(thumbnailCmd).inheritIO().start();

            if (process.waitFor() == 0) {
                s3Service.uploadThumbnail(streamWatchKey, outputPath);
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

