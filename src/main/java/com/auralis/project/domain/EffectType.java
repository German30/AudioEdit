package com.auralis.project.domain;

/**
 * Supported DSP effects. Each maps to an ffmpeg filter (or filter chain) at
 * render time; {@code paramsJson} on the {@link Effect} carries the knobs.
 */
public enum EffectType {
    GAIN,        // volume=<gainDb>dB
    NORMALIZE,   // loudnorm (EBU R128) or dynaudnorm
    FADE,        // afade in/out (params: type,start,duration,curve)
    EQ,          // equalizer / anequalizer (params: bands[])
    LOWPASS,     // lowpass=f=<hz>
    HIGHPASS,    // highpass=f=<hz>
    REVERB,      // aecho / afir impulse (params: delay,decay,wet)
    COMPRESSOR,  // acompressor (params: threshold,ratio,attack,release)
    PITCH,       // rubberband/asetrate+aresample (params: semitones)
    TEMPO        // atempo chain (params: ratio)
}
