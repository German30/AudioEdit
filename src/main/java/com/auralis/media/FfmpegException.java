package com.auralis.media;

/** Raised when an ffmpeg/ffprobe invocation exits non-zero or times out. */
public class FfmpegException extends RuntimeException {

    private final int exitCode;
    private final String stderrTail;

    public FfmpegException(String message, int exitCode, String stderrTail) {
        super(message + (stderrTail == null || stderrTail.isBlank() ? "" : " | stderr: " + stderrTail));
        this.exitCode = exitCode;
        this.stderrTail = stderrTail;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStderrTail() {
        return stderrTail;
    }
}
