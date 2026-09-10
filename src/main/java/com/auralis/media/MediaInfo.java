package com.auralis.media;

/**
 * The subset of ffprobe output the platform cares about.
 *
 * @param durationSeconds total duration
 * @param sampleRate      Hz of the first audio stream
 * @param channels        channel count of the first audio stream
 * @param codec           codec name (e.g. {@code mp3}, {@code pcm_s16le}, {@code aac})
 * @param bitRateKbps     nominal bit rate in kbps, or {@code 0} if unknown
 * @param formatName      container/format long name
 */
public record MediaInfo(
        double durationSeconds,
        int sampleRate,
        int channels,
        String codec,
        int bitRateKbps,
        String formatName
) {
}
