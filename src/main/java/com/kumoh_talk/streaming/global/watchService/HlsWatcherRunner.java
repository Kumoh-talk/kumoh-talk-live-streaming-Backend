package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.util.FfmpegExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.DESKTOP_TYPE;
import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.STREAMING_TYPE_DELIMITER;

@Component
@RequiredArgsConstructor
public class HlsWatcherRunner {

    private static final ConcurrentHashMap<String, HlsWatcher> HLS_WATCHER_MAP = new ConcurrentHashMap<>();

    private final FfmpegExecutor ffmpegExecutor;
    private final S3Service s3Service;

    @Async
    public void startWatcher(Path dirPath, String streamWatchKey, String type) {
        HlsWatcher.HlsStreamStartedEventHandler hlsStreamStartedEventHandler = getHlsStreamStartedEventHandler(type);

        HlsWatcher watcher = HlsWatcher.builder()
                .directoryPath(dirPath)
                .streamWatchKey(streamWatchKey)
                .tsFileEventHandler(s3Service::uploadHlsFile)
                .hlsStreamStartedEventHandler(hlsStreamStartedEventHandler)
                .build();

        Thread watcherThread = new Thread(watcher);
        watcherThread.setDaemon(true);
        watcherThread.start();

        HLS_WATCHER_MAP.put(streamWatchKey + STREAMING_TYPE_DELIMITER + type, watcher);
    }

    private HlsWatcher.HlsStreamStartedEventHandler getHlsStreamStartedEventHandler(String type) {
        HlsWatcher.HlsStreamStartedEventHandler hlsStreamStartedEventHandler;
        if (type.equals(DESKTOP_TYPE)) {
            hlsStreamStartedEventHandler = (filePath, streamWatchKey) -> {
                    ffmpegExecutor.extractThumbnail(filePath, streamWatchKey);
                    ffmpegExecutor.startAudioFfmpeg(streamWatchKey);
            };
        } else {
            hlsStreamStartedEventHandler = ffmpegExecutor::extractThumbnail;
        }
        return hlsStreamStartedEventHandler;
    }

    public void stopWatcher(String streamWatchKey, String type) {
        String mapKey = streamWatchKey + STREAMING_TYPE_DELIMITER + type;
        HlsWatcher watcher = HLS_WATCHER_MAP.get(mapKey);

        if (watcher != null) {
            watcher.stopWatching();
        }

        HLS_WATCHER_MAP.remove(mapKey);
    }
}