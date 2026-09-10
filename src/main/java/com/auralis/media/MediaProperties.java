package com.auralis.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "auralis.media")
public record MediaProperties(
        String ffmpegPath,
        String ffprobePath,
        Path workDir,
        int waveformPeaks
) {
}
