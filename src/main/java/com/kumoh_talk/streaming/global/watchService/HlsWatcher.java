package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.kumoh_talk.streaming.global.constant.StreamingConstants.HLS_TIME;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

@Slf4j
public class HlsWatcher implements Runnable {

    private final Path pathToWatch;
    private final FileEventHandler fileEventHandler;
    private volatile boolean watching;

    @Builder
    public HlsWatcher(String directoryPath, FileEventHandler fileEventHandler) {
        this.pathToWatch = Paths.get(directoryPath);
        this.fileEventHandler = fileEventHandler;
        this.watching = true;
    }

    @Override
    public void run() {
        waitForHlsSegment(Duration.ofSeconds(40));

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathToWatch.register(watchService, ENTRY_CREATE);

            while (watching) {
                WatchKey key = watchService.poll(HLS_TIME * 3L + 10, TimeUnit.SECONDS);
                if (key == null) {
                    log.info("파일 생성 감지 자동 종료: {}", pathToWatch);
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind != ENTRY_CREATE) {
                        continue;
                    }

                    Path createdFilePath = pathToWatch.resolve((Path) event.context());
                    if (!createdFilePath.toString().endsWith(".ts")) {
                        continue;
                    }

                    fileEventHandler.handleNewFile(createdFilePath);
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
                if (stream.iterator().hasNext()) {
                    log.info("{}: HLS 세그먼트 감지됨", pathToWatch);
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

    public interface FileEventHandler {
        void handleNewFile(Path filePath);
    }

    public void stopWatching() {
        watching = false;
    }
}