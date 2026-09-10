package com.auralis.render.engine;

import com.auralis.project.domain.EffectType;
import com.auralis.project.graph.ProjectGraph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FilterGraphBuilderTest {

    private final FilterGraphBuilder builder = new FilterGraphBuilder();

    @Test
    void buildsChainForSingleClipWithSpeedAndFade() {
        UUID assetId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();

        var clip = new ProjectGraph.ClipNode(UUID.randomUUID(), assetId, "users/x/assets/a/source/a.wav", "a.wav",
                2.0, 1.0, 4.0, -6.0, 0.5, 1.0, 2.0, false, List.of());
        var track = new ProjectGraph.TrackNode(trackId, "Drums", 0, 0.0, 0.0, false, false,
                List.of(clip), List.of());
        var graph = new ProjectGraph(UUID.randomUUID(), "demo", 48000, null, 0.0, List.of(track), List.of());

        var built = builder.build(graph, Map.of(assetId, Path.of("/tmp/a.wav")), 48000, false);

        assertThat(built.inputs()).containsExactly(Path.of("/tmp/a.wav"));
        assertThat(built.filterComplex())
                .contains("atrim=start=1:end=9")     // offset 1s + (4s * rate 2.0)
                .contains("atempo=2")                // playbackRate 2.0
                .contains("afade=t=in:st=0:d=0.5")
                .contains("adelay=2000:all=1")       // timelineStart 2.0s
                .contains("[mix]");
    }

    @Test
    void mutedAndNonSoloedTracksAreExcluded() {
        UUID assetId = UUID.randomUUID();
        var clip = new ProjectGraph.ClipNode(UUID.randomUUID(), assetId, "k", "k.wav",
                0, 0, 1, 0, 0, 0, 1.0, false, List.of());
        var soloed = new ProjectGraph.TrackNode(UUID.randomUUID(), "solo", 0, 0, 0, false, true, List.of(clip), List.of());
        var other = new ProjectGraph.TrackNode(UUID.randomUUID(), "other", 1, 0, 0, false, false, List.of(clip), List.of());
        var graph = new ProjectGraph(UUID.randomUUID(), "d", 48000, null, 0.0, List.of(soloed, other), List.of());

        var built = builder.build(graph, Map.of(assetId, Path.of("/tmp/k.wav")), 48000, false);

        assertThat(built.filterComplex()).contains("[t0]").doesNotContain("[t1]");
    }

    @Test
    void masterEffectAppearsInMasterChain() {
        UUID assetId = UUID.randomUUID();
        var clip = new ProjectGraph.ClipNode(UUID.randomUUID(), assetId, "k", "k.wav",
                0, 0, 1, 0, 0, 0, 1.0, false, List.of());
        var track = new ProjectGraph.TrackNode(UUID.randomUUID(), "t", 0, 0, 0, false, false, List.of(clip), List.of());
        var master = new ProjectGraph.EffectNode(UUID.randomUUID(), EffectType.NORMALIZE, true, Map.of());
        var graph = new ProjectGraph(UUID.randomUUID(), "d", 44100, null, -3.0, List.of(track), List.of(master));

        var built = builder.build(graph, Map.of(assetId, Path.of("/tmp/k.wav")), 44100, false);

        assertThat(built.filterComplex()).contains("loudnorm").contains("volume=0.707946");
    }
}
