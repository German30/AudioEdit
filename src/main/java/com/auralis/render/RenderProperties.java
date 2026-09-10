package com.auralis.render;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auralis.render")
public record RenderProperties(
        int workerThreads,
        Duration pollInterval,
        int maxAttempts
) {
}
