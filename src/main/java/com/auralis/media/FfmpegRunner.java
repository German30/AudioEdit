package com.auralis.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Thin, synchronous wrapper around the {@code ffmpeg} / {@code ffprobe} binaries.
 * Everything is a plain {@link ProcessBuilder} call so we keep full control over
 * the filter graph and codec flags.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegRunner {

    private static final int STDERR_TAIL_LINES = 40;

    private final MediaProperties props;

    /** Run ffmpeg with the given args (without the leading binary). */
    public void ffmpeg(List<String> args, Duration timeout) {
        run(prepend(props.ffmpegPath(), args), timeout, false);
    }

    /** Run ffprobe and return its stdout (typically JSON). */
    public String ffprobe(List<String> args, Duration timeout) {
        return run(prepend(props.ffprobePath(), args), timeout, true);
    }

    private String run(List<String> command, Duration timeout, boolean captureStdout) {
        log.debug("exec: {}", String.join(" ", command));
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            throw new FfmpegException("Failed to start " + command.get(0) + " (is it on PATH?)", -1, e.getMessage());
        }

        StringBuilder stdout = new StringBuilder();
        Deque<String> stderrTail = new ArrayDeque<>();
        Thread outReader = pump(process.getInputStream(), line -> {
            if (captureStdout) stdout.append(line).append('\n');
        });
        Thread errReader = pump(process.getErrorStream(), line -> {
            stderrTail.addLast(line);
            if (stderrTail.size() > STDERR_TAIL_LINES) stderrTail.removeFirst();
        });

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new FfmpegException("Interrupted while waiting for " + command.get(0), -1, null);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new FfmpegException(command.get(0) + " timed out after " + timeout, -1, String.join("\n", stderrTail));
        }
        join(outReader);
        join(errReader);

        int exit = process.exitValue();
        if (exit != 0) {
            throw new FfmpegException(command.get(0) + " exited with code " + exit, exit, String.join("\n", stderrTail));
        }
        return stdout.toString();
    }

    private static Thread pump(java.io.InputStream in, java.util.function.Consumer<String> sink) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sink.accept(line);
            } catch (IOException ignored) {
                // stream closed with the process
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void join(Thread t) {
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> prepend(String binary, List<String> args) {
        List<String> full = new ArrayList<>(args.size() + 1);
        full.add(binary);
        full.addAll(args);
        return full;
    }
}
