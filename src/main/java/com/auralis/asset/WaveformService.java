package com.auralis.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auralis.media.FfmpegRunner;
import com.auralis.media.MediaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Builds a compact min/max peak list for waveform rendering in the editor.
 * Audio is decoded to mono 8 kHz signed-16 PCM (ample for a visual overview)
 * and reduced to a fixed number of peak pairs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaveformService {

    private static final int DECODE_RATE = 8000;
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final FfmpegRunner ffmpeg;
    private final MediaProperties props;
    private final ObjectMapper objectMapper;

    /** @return JSON bytes: {@code {version,sampleRate,peakCount,peaks:[min,max,...]}} with peaks in [-1,1]. */
    public byte[] generate(Path audioFile) throws IOException {
        int peakCount = Math.max(props.waveformPeaks(), 100);
        Path raw = Files.createTempFile(props.workDir(), "wf-", ".s16le");
        try {
            ffmpeg.ffmpeg(List.of(
                    "-y",
                    "-i", audioFile.toString(),
                    "-ac", "1",
                    "-ar", String.valueOf(DECODE_RATE),
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    raw.toString()), TIMEOUT);

            long totalSamples = Files.size(raw) / 2;
            if (totalSamples <= 0) {
                return objectMapper.writeValueAsBytes(new Waveform(1, DECODE_RATE, 0, new double[0]));
            }
            int buckets = (int) Math.min(peakCount, totalSamples);
            long samplesPerBucket = Math.max(1, totalSamples / buckets);

            double[] peaks = new double[buckets * 2];
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(raw)))) {
                for (int b = 0; b < buckets; b++) {
                    short min = Short.MAX_VALUE;
                    short max = Short.MIN_VALUE;
                    long limit = (b == buckets - 1) ? totalSamples - (long) b * samplesPerBucket : samplesPerBucket;
                    for (long i = 0; i < limit; i++) {
                        short s = readLeShort(in);
                        if (s < min) min = s;
                        if (s > max) max = s;
                    }
                    peaks[b * 2] = min / 32768.0;
                    peaks[b * 2 + 1] = max / 32768.0;
                }
            }
            return objectMapper.writeValueAsBytes(new Waveform(1, DECODE_RATE, buckets, peaks));
        } finally {
            Files.deleteIfExists(raw);
        }
    }

    private static short readLeShort(InputStream in) throws IOException {
        int lo = in.read();
        int hi = in.read();
        if (lo < 0 || hi < 0) throw new EOFException();
        return (short) ((hi << 8) | lo);
    }

    private record Waveform(int version, int sampleRate, int peakCount, double[] peaks) {}
}
