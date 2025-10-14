package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.HLS_TIME;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Slf4j
public class HlsWatcher implements Runnable {

    private final Path pathToWatch;
    private final String streamWatchKey;
    private final String streamUploadKey;
    private final TsFileEventHandler tsFileEventHandler;
    private final HlsStreamStartedEventHandler hlsStreamStartedEventHandler;
    private volatile boolean watching;

    @Builder
    public HlsWatcher(Path directoryPath, String streamWatchKey, TsFileEventHandler tsFileEventHandler, HlsStreamStartedEventHandler hlsStreamStartedEventHandler) {
        this.pathToWatch = directoryPath;
        this.streamWatchKey = streamWatchKey;
        this.streamUploadKey = directoryPath.getFileName().toString();
        this.tsFileEventHandler = tsFileEventHandler;
        this.hlsStreamStartedEventHandler = hlsStreamStartedEventHandler;
        this.watching = true;
    }

    @Override
    public void run() {
        waitForHlsSegment(Duration.ofSeconds(20));

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathToWatch.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);

            while (watching) {
                WatchKey key = watchService.poll(HLS_TIME * 3L + 10, TimeUnit.SECONDS);
                if (key == null) {
                    log.info("파일 생성 감지 자동 종료: {}", pathToWatch);
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind != ENTRY_CREATE && kind != ENTRY_MODIFY) {
                        continue;
                    }

                    String filename = event.context().toString();
                    if (!filename.endsWith(".ts")) {
                        continue;
                    }

                    Path filePath = pathToWatch.resolve(filename);
                    tsFileEventHandler.handleNewFile(filePath, streamWatchKey);
                }

                if (!key.reset()) {
                    throw ServiceException.from(ExceptionCode.DIRECTORY_WATCH_FAILED);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw ServiceException.from(ExceptionCode.UNEXPECTED_SERVER_ERROR);
        }
    }

    private void waitForHlsSegment(Duration timeout) {
        Instant start = Instant.now();

        while (Duration.between(start, Instant.now()).compareTo(timeout) < 0) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pathToWatch, "*.ts")) {
                Iterator<Path> iterator = stream.iterator();
                if (iterator.hasNext()) {
                    Path tsFilePath = iterator.next();
                    log.info("{}: HLS 세그먼트 감지됨", tsFilePath);
                    hlsStreamStartedEventHandler.handleStreamStart(tsFilePath, streamUploadKey, streamWatchKey);
                    return;
                }
            } catch (IOException e) {
                log.error("HLS 디렉토리 접근 실패: {}", e.getMessage());
            }
            threadSleep(500);
        }

        log.error("{}: HLS time out", pathToWatch);
        throw ServiceException.from(ExceptionCode.HLS_STREAM_TIMEOUT);
    }

    private void threadSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("스레드 대기 실패: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    public interface TsFileEventHandler {
        void handleNewFile(Path filePath, String streamWatchKey);
    }

    @FunctionalInterface
    public interface HlsStreamStartedEventHandler {
        void handleStreamStart(Path filePath, String streamUploadKey, String streamWatchKey);
    }

    public void stopWatching() {
        watching = false;
    }
}