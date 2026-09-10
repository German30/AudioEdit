package com.auralis.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Extracts {@link MediaInfo} from a local media file via {@code ffprobe}.
 */
@Service
@RequiredArgsConstructor
public class ProbeService {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(30);

    private final FfmpegRunner runner;
    private final ObjectMapper objectMapper;

    public MediaInfo probe(Path file) {
        String json = runner.ffprobe(List.of(
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                file.toString()), PROBE_TIMEOUT);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new FfmpegException("Could not parse ffprobe output", -1, e.getMessage());
        }

        JsonNode format = root.path("format");
        JsonNode audio = null;
        for (JsonNode stream : root.path("streams")) {
            if ("audio".equals(stream.path("codec_type").asText())) {
                audio = stream;
                break;
            }
        }
        if (audio == null) {
            throw new FfmpegException("File contains no audio stream", -1, null);
        }

        double duration = format.path("duration").asDouble(audio.path("duration").asDouble(0));
        int sampleRate = audio.path("sample_rate").asInt(0);
        int channels = audio.path("channels").asInt(0);
        String codec = audio.path("codec_name").asText("unknown");
        long bitRate = format.path("bit_rate").asLong(audio.path("bit_rate").asLong(0));
        String formatName = format.path("format_long_name").asText(format.path("format_name").asText("unknown"));

        return new MediaInfo(duration, sampleRate, channels, codec, (int) (bitRate / 1000), formatName);
    }
}
