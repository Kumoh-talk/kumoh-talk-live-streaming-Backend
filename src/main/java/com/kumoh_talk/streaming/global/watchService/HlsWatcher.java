package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static com.kumoh_talk.streaming.global.constant.StreamingConstants.HLS_TIME;
import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
public class HlsWatcher implements Runnable {

    private final Path pathToWatch;
    private final FileEventHandler fileEventHandler;
    private volatile boolean watching;

    public HlsWatcher(String directoryPath, FileEventHandler fileEventHandler) {
        this.pathToWatch = Paths.get(directoryPath);
        this.fileEventHandler = fileEventHandler;
        this.watching = true;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathToWatch.register(watchService, ENTRY_CREATE);

            while (watching) {
                WatchKey key = watchService.poll(HLS_TIME * 2, TimeUnit.SECONDS);
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

    public interface FileEventHandler {
        void handleNewFile(Path filePath);
    }

    public void stopWatching() {
        watching = false;
    }
}