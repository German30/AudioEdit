package com.auralis.render.engine;

import com.auralis.project.graph.ProjectGraph;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Compiles a {@link ProjectGraph} into a single ffmpeg {@code -filter_complex}
 * expression plus the ordered list of input files. One input per referenced
 * asset; every clip becomes a sub-chain that is trimmed, sped up / reversed,
 * faded, gained and delayed to its timeline position, then mixed per track and
 * finally down to a master bus.
 */
@Component
public class FilterGraphBuilder {

    public record BuiltGraph(
            List<Path> inputs,
            String filterComplex,
            String masterLabel,
            Map<UUID, String> stemLabels,
            double estimatedDurationSeconds) {}

    public BuiltGraph build(ProjectGraph graph, Map<UUID, Path> assetFiles, int sampleRate, boolean withStems) {
        // 1. assign an input index to every asset actually used
        List<Path> inputs = new ArrayList<>();
        Map<UUID, Integer> inputIndex = new LinkedHashMap<>();
        for (ProjectGraph.TrackNode track : graph.tracks()) {
            for (ProjectGraph.ClipNode clip : track.clips()) {
                inputIndex.computeIfAbsent(clip.sourceAssetId(), id -> {
                    inputs.add(assetFiles.get(id));
                    return inputs.size() - 1;
                });
            }
        }

        boolean anySolo = graph.hasAnySolo();
        List<String> chains = new ArrayList<>();
        List<String> trackLabels = new ArrayList<>();
        Map<UUID, String> stemLabels = new LinkedHashMap<>();

        int t = 0;
        for (ProjectGraph.TrackNode track : graph.tracks()) {
            if (track.muted() || (anySolo && !track.soloed()) || track.clips().isEmpty()) {
                t++;
                continue;
            }
            List<String> clipLabels = new ArrayList<>();
            int k = 0;
            for (ProjectGraph.ClipNode clip : track.clips()) {
                String label = "c%d_%d".formatted(t, k);
                chains.add(clipChain(clip, inputIndex.get(clip.sourceAssetId()), sampleRate, label));
                clipLabels.add(label);
                k++;
            }
            String trackLabel = "t%d".formatted(t);
            chains.add(trackChain(track, clipLabels, sampleRate, trackLabel));
            trackLabels.add(trackLabel);
            if (withStems) {
                stemLabels.put(track.id(), trackLabel);
            }
            t++;
        }

        String masterLabel = "mix";
        if (trackLabels.isEmpty()) {
            chains.add("anullsrc=r=%d:cl=stereo,atrim=0:1[%s]".formatted(sampleRate, masterLabel));
        } else {
            String join = trackLabels.stream().map(l -> "[" + l + "]").reduce("", String::concat);
            String masterFx = EffectFilters.chainFor(graph.masterEffects(), sampleRate);
            StringBuilder master = new StringBuilder(join)
                    .append("amix=inputs=").append(trackLabels.size()).append(":normalize=0:dropout_transition=0");
            if (!masterFx.isBlank()) master.append(',').append(masterFx);
            double masterGain = EffectFilters.linear(graph.masterGainDb());
            if (Math.abs(masterGain - 1.0) > 1e-6) master.append(",volume=").append(EffectFilters.fmt(masterGain));
            master.append('[').append(masterLabel).append(']');
            chains.add(master.toString());
        }

        return new BuiltGraph(inputs, String.join(";", chains), masterLabel, stemLabels,
                graph.totalDurationSeconds());
    }

    private String clipChain(ProjectGraph.ClipNode clip, int inputIdx, int sampleRate, String outLabel) {
        double rate = clip.playbackRate() <= 0 ? 1.0 : clip.playbackRate();
        double srcSpan = Math.max(clip.durationSeconds() * rate, 0.001);
        double off = Math.max(clip.sourceOffsetSeconds(), 0);

        StringBuilder c = new StringBuilder("[%d:a]".formatted(inputIdx));
        c.append("atrim=start=").append(EffectFilters.fmt(off))
                .append(":end=").append(EffectFilters.fmt(off + srcSpan))
                .append(",asetpts=PTS-STARTPTS");
        if (clip.reversed()) {
            c.append(",areverse");
        }
        String tempo = AtempoChain.of(rate);
        if (!tempo.isBlank()) {
            c.append(',').append(tempo);
        }
        String clipFx = EffectFilters.chainFor(clip.effects(), sampleRate);
        if (!clipFx.isBlank()) {
            c.append(',').append(clipFx);
        }
        c.append(",aformat=sample_rates=").append(sampleRate).append(":channel_layouts=stereo");
        if (clip.fadeInSeconds() > 0) {
            c.append(",afade=t=in:st=0:d=").append(EffectFilters.fmt(clip.fadeInSeconds()));
        }
        if (clip.fadeOutSeconds() > 0) {
            double st = Math.max(0, clip.durationSeconds() - clip.fadeOutSeconds());
            c.append(",afade=t=out:st=").append(EffectFilters.fmt(st))
                    .append(":d=").append(EffectFilters.fmt(clip.fadeOutSeconds()));
        }
        double gain = EffectFilters.linear(clip.gainDb());
        if (Math.abs(gain - 1.0) > 1e-6) {
            c.append(",volume=").append(EffectFilters.fmt(gain));
        }
        long delayMs = Math.round(clip.timelineStartSeconds() * 1000.0);
        if (delayMs > 0) {
            c.append(",adelay=").append(delayMs).append(":all=1");
        }
        c.append('[').append(outLabel).append(']');
        return c.toString();
    }

    private String trackChain(ProjectGraph.TrackNode track, List<String> clipLabels, int sampleRate, String outLabel) {
        String join = clipLabels.stream().map(l -> "[" + l + "]").reduce("", String::concat);
        StringBuilder tc = new StringBuilder(join)
                .append("amix=inputs=").append(clipLabels.size()).append(":normalize=0:dropout_transition=0");

        String trackFx = EffectFilters.chainFor(track.effects(), sampleRate);
        if (!trackFx.isBlank()) tc.append(',').append(trackFx);

        if (Math.abs(track.pan()) > 1e-6) {
            double p = Math.max(-1, Math.min(1, track.pan()));
            double l = p <= 0 ? 1.0 : 1.0 - p;
            double r = p >= 0 ? 1.0 : 1.0 + p;
            tc.append(",pan=stereo|c0=%s*c0|c1=%s*c1".formatted(EffectFilters.fmt(l), EffectFilters.fmt(r)));
        }
        double gain = EffectFilters.linear(track.gainDb());
        if (Math.abs(gain - 1.0) > 1e-6) {
            tc.append(",volume=").append(EffectFilters.fmt(gain));
        }
        tc.append('[').append(outLabel).append(']');
        return tc.toString();
    }

    static String fmt(double d) {
        return String.format(Locale.ROOT, "%.6f", d);
    }
}
