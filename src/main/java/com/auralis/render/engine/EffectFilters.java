package com.auralis.render.engine;

import com.auralis.project.domain.EffectType;
import com.auralis.project.graph.ProjectGraph.EffectNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps an {@link EffectNode} to the ffmpeg audio-filter fragment(s) that realise
 * it. Returns an empty string for a disabled or unmapped effect. Fragments are
 * comma-joinable into a bigger filter chain.
 */
final class EffectFilters {

    private EffectFilters() {}

    static String chainFor(List<EffectNode> effects, int sampleRate) {
        if (effects == null || effects.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (EffectNode e : effects) {
            String frag = single(e, sampleRate);
            if (!frag.isBlank()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(frag);
            }
        }
        return sb.toString();
    }

    private static String single(EffectNode e, int sampleRate) {
        if (!e.enabled()) return "";
        Map<String, Object> p = e.params() == null ? Map.of() : e.params();
        return switch (e.type()) {
            case GAIN -> "volume=" + linear(num(p, "gainDb", 0));
            case NORMALIZE -> "loudnorm=I=%s:TP=%s:LRA=%s".formatted(
                    fmt(num(p, "targetLufs", -14)), fmt(num(p, "truePeak", -1.5)), fmt(num(p, "range", 11)));
            case FADE -> "afade=t=%s:st=%s:d=%s:curve=%s".formatted(
                    str(p, "direction", "in"), fmt(num(p, "start", 0)), fmt(num(p, "duration", 1)),
                    str(p, "curve", "tri"));
            case LOWPASS -> "lowpass=f=" + fmt(num(p, "frequency", 12000));
            case HIGHPASS -> "highpass=f=" + fmt(num(p, "frequency", 80));
            case EQ -> eqChain(p);
            case REVERB -> "aecho=%s:%s:%s:%s".formatted(
                    fmt(num(p, "inGain", 0.8)), fmt(num(p, "outGain", 0.9)),
                    fmt(num(p, "delayMs", 60)), fmt(num(p, "decay", 0.4)));
            case COMPRESSOR -> "acompressor=threshold=%sdB:ratio=%s:attack=%s:release=%s:makeup=%s".formatted(
                    fmt(num(p, "thresholdDb", -18)), fmt(num(p, "ratio", 3)),
                    fmt(num(p, "attackMs", 20)), fmt(num(p, "releaseMs", 250)), fmt(num(p, "makeupDb", 0)));
            case PITCH -> pitchChain(num(p, "semitones", 0), sampleRate);
            case TEMPO -> AtempoChain.of(num(p, "ratio", 1.0));
        };
    }

    private static String eqChain(Map<String, Object> p) {
        Object bands = p.get("bands");
        if (!(bands instanceof List<?> list) || list.isEmpty()) {
            return "equalizer=f=%s:width_type=o:width=%s:g=%s".formatted(
                    fmt(num(p, "frequency", 1000)), fmt(num(p, "width", 1)), fmt(num(p, "gainDb", 0)));
        }
        StringBuilder sb = new StringBuilder();
        for (Object b : list) {
            if (!(b instanceof Map<?, ?> band)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append("equalizer=f=%s:width_type=o:width=%s:g=%s".formatted(
                    fmt(toD(band.get("f"), 1000)), fmt(toD(band.get("width"), 1)), fmt(toD(band.get("gain"), 0))));
        }
        return sb.toString();
    }

    private static String pitchChain(double semitones, int sampleRate) {
        if (semitones == 0) return "";
        double factor = Math.pow(2.0, semitones / 12.0);
        return "asetrate=%d*%s,aresample=%d,%s".formatted(
                sampleRate, fmt(factor), sampleRate, AtempoChain.of(1.0 / factor));
    }

    // ---- helpers ----
    static double linear(double db) {
        return Math.pow(10.0, db / 20.0);
    }

    private static double num(Map<String, Object> p, String key, double dflt) {
        return toD(p.get(key), dflt);
    }

    private static double toD(Object v, double dflt) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return dflt;
            }
        }
        return dflt;
    }

    private static String str(Map<String, Object> p, String key, String dflt) {
        Object v = p.get(key);
        return v == null ? dflt : v.toString();
    }

    static String fmt(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return Long.toString((long) d);
        }
        return String.format(Locale.ROOT, "%.6f", d);
    }
}
