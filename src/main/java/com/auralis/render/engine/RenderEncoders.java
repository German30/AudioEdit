package com.auralis.render.engine;

import com.auralis.common.error.ApiException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Container/codec selection for a target export format.
 */
public final class RenderEncoders {

    public static final Set<String> SUPPORTED = Set.of("mp3", "wav", "flac", "aac", "ogg");

    private RenderEncoders() {}

    public static String normalize(String format) {
        String f = format == null ? "mp3" : format.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(f)) {
            throw ApiException.badRequest("Unsupported export format: " + format + " (supported: " + SUPPORTED + ")");
        }
        return f;
    }

    public static String extension(String normalizedFormat) {
        return normalizedFormat.equals("aac") ? "m4a" : normalizedFormat;
    }

    /** ffmpeg output args (codec + bitrate + rate), excluding the output path. */
    public static List<String> outputArgs(String normalizedFormat, int bitrateKbps, int sampleRate) {
        String br = bitrateKbps + "k";
        return switch (normalizedFormat) {
            case "mp3" -> List.of("-c:a", "libmp3lame", "-b:a", br, "-ar", String.valueOf(sampleRate), "-ac", "2");
            case "wav" -> List.of("-c:a", "pcm_s16le", "-ar", String.valueOf(sampleRate), "-ac", "2");
            case "flac" -> List.of("-c:a", "flac", "-ar", String.valueOf(sampleRate), "-ac", "2");
            case "aac" -> List.of("-c:a", "aac", "-b:a", br, "-ar", String.valueOf(sampleRate), "-ac", "2",
                    "-movflags", "+faststart");
            case "ogg" -> List.of("-c:a", "libvorbis", "-b:a", br, "-ar", String.valueOf(sampleRate), "-ac", "2");
            default -> throw ApiException.badRequest("Unsupported export format: " + normalizedFormat);
        };
    }
}
