package com.kumoh_talk.streaming.global.watchService;

import com.kumoh_talk.streaming.global.file.service.S3Service;
import com.kumoh_talk.streaming.global.util.FfmpegExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.DESKTOP_TYPE;

@Component
@RequiredArgsConstructor
public class HlsWatcherRunner {

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
    }

    private HlsWatcher.HlsStreamStartedEventHandler getHlsStreamStartedEventHandler(String type) {
        HlsWatcher.HlsStreamStartedEventHandler hlsStreamStartedEventHandler;
        if (type.equals(DESKTOP_TYPE)) {
            hlsStreamStartedEventHandler = (filePath, streamUploadKey, streamWatchKey) -> {
                    ffmpegExecutor.extractThumbnail(filePath, streamWatchKey);
                    ffmpegExecutor.startAudioFfmpeg(streamUploadKey, streamWatchKey);
            };
        } else {
            hlsStreamStartedEventHandler = (filePath, streamUploadKey, streamWatchKey) ->
                    ffmpegExecutor.extractThumbnail(filePath, streamWatchKey);
        }
        return hlsStreamStartedEventHandler;
    }
}