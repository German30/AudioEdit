package com.auralis.render.engine;

import java.util.Locale;

/**
 * ffmpeg's {@code atempo} filter only accepts factors in [0.5, 2.0], so a larger
 * speed change has to be expressed as a chain of atempo filters whose product is
 * the desired ratio.
 */
final class AtempoChain {

    private AtempoChain() {}

    static String of(double ratio) {
        if (ratio <= 0 || Math.abs(ratio - 1.0) < 1e-6) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        double remaining = ratio;
        while (remaining > 2.0 + 1e-9) {
            append(sb, 2.0);
            remaining /= 2.0;
        }
        while (remaining < 0.5 - 1e-9) {
            append(sb, 0.5);
            remaining /= 0.5;
        }
        append(sb, remaining);
        return sb.toString();
    }

    private static void append(StringBuilder sb, double factor) {
        if (sb.length() > 0) sb.append(',');
        sb.append("atempo=").append(String.format(Locale.ROOT, "%.6f", factor));
    }
}
