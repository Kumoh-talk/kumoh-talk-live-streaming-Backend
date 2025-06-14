package com.kumoh_talk.streaming.domain.stream.constant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "streaming")
@Getter
@Setter
public class StreamingConfig {
    private String hlsUrlPrefix;
    private String audioApiUrl;
}