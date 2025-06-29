package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.util.FfmpegExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.DESKTOP_TYPE;

@Component
@RequiredArgsConstructor
public class HlsWatcherRunner {

    private static final HlsWatcher.ThumbnailEventHandler NOOP_THUMBNAIL_HANDLER = (path, streamWatchKey) -> {};

    private final FfmpegExecutor ffmpegExecutor;
    private final S3Service s3Service;

    @Async
    public void startWatcher(String streamWatchKey, String type) {
        HlsWatcher.ThumbnailEventHandler thumbnailHandler;
        if (type.equals(DESKTOP_TYPE)) {
            thumbnailHandler = ffmpegExecutor::extractThumbnail;
        } else {
            thumbnailHandler = NOOP_THUMBNAIL_HANDLER;
        }

        HlsWatcher watcher = HlsWatcher.builder()
                .streamWatchKey(streamWatchKey)
                .fileEventHandler(s3Service::uploadHlsFile)
                .thumbnailEventHandler(thumbnailHandler)
                .build();

        Thread watcherThread = new Thread(watcher);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
}