package com.auralis.render;

import com.auralis.media.FfmpegRunner;
import com.auralis.media.MediaProperties;
import com.auralis.media.ProbeService;
import com.auralis.project.graph.GraphAssembler;
import com.auralis.project.graph.ProjectGraph;
import com.auralis.render.RenderJobStore.JobContext;
import com.auralis.render.engine.FilterGraphBuilder;
import com.auralis.render.engine.RenderEncoders;
import com.auralis.storage.StorageKeys;
import com.auralis.storage.StorageService;
import com.auralis.subscription.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Executes one claimed render job: rehydrate the frozen graph, pull the source
 * files, compile a single ffmpeg {@code -filter_complex} pass, upload the result
 * and record usage. Transactional DB work is delegated to {@link RenderJobStore}
 * so the long ffmpeg call is never inside a transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenderExecutionService {

    private final RenderJobStore store;
    private final GraphAssembler graphAssembler;
    private final FilterGraphBuilder filterGraphBuilder;
    private final FfmpegRunner ffmpeg;
    private final ProbeService probeService;
    private final StorageService storage;
    private final QuotaService quotaService;
    private final MediaProperties mediaProps;

    public void execute(UUID jobId) {
        Optional<JobContext> maybeCtx = store.loadRunnableContext(jobId);
        if (maybeCtx.isEmpty()) {
            return;
        }
        JobContext ctx = maybeCtx.get();
        Path workDir = mediaProps.workDir().resolve("job-" + jobId);
        try {
            Files.createDirectories(workDir);
            ProjectGraph graph = graphAssembler.fromJson(ctx.graphJson());

            Map<UUID, Path> assetFiles = downloadAssets(graph, workDir);
            String format = RenderEncoders.normalize(ctx.format());
            String ext = RenderEncoders.extension(format);

            FilterGraphBuilder.BuiltGraph built =
                    filterGraphBuilder.build(graph, assetFiles, ctx.sampleRate(), ctx.includeStems());

            Path master = workDir.resolve("master." + ext);
            runFfmpeg(built, "[" + built.masterLabel() + "]", ctx, format, master, built.estimatedDurationSeconds());

            double renderedSeconds = safeDuration(master, built.estimatedDurationSeconds());
            String outputKey = StorageKeys.renderOutput(ctx.projectId(), jobId, ext);
            storage.upload(outputKey, master, "audio/" + format);
            long outputSize = Files.size(master);

            String stemsKey = null;
            long stemsSize = 0;
            if (ctx.includeStems() && !built.stemLabels().isEmpty()) {
                Path zip = buildStems(built, ctx, format, ext, workDir);
                stemsKey = StorageKeys.renderStemZip(ctx.projectId(), jobId);
                storage.upload(stemsKey, zip, "application/zip");
                stemsSize = Files.size(zip);
            }

            long billed = (long) Math.ceil(renderedSeconds);
            store.markSucceeded(jobId, outputKey, outputSize, stemsKey, renderedSeconds, billed);
            quotaService.recordRenderUsage(ctx.requestedByUserId(), billed);
            quotaService.addStorageUsage(ctx.requestedByUserId(), outputSize + stemsSize);
            log.info("Render {} succeeded: {} ({}s, {} bytes)", jobId, format,
                    String.format("%.1f", renderedSeconds), outputSize);

        } catch (Exception e) {
            log.warn("Render {} failed on this attempt", jobId, e);
            store.markFailedOrRetry(jobId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            deleteTree(workDir);
        }
    }

    private void runFfmpeg(FilterGraphBuilder.BuiltGraph built, String mapLabel, JobContext ctx,
                           String format, Path output, double estimatedSeconds) {
        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("-nostdin");
        for (Path in : built.inputs()) {
            args.add("-i");
            args.add(in.toString());
        }
        args.add("-filter_complex");
        args.add(built.filterComplex());
        args.add("-map");
        args.add(mapLabel);
        args.addAll(RenderEncoders.outputArgs(format, ctx.bitrateKbps(), ctx.sampleRate()));
        args.add(output.toString());

        Duration timeout = Duration.ofSeconds(Math.max(300, (long) (estimatedSeconds * 4) + 120));
        ffmpeg.ffmpeg(args, timeout);
    }

    private Path buildStems(FilterGraphBuilder.BuiltGraph built, JobContext ctx, String format, String ext, Path workDir)
            throws IOException {
        List<Path> stemFiles = new ArrayList<>();
        int i = 0;
        for (Map.Entry<UUID, String> entry : built.stemLabels().entrySet()) {
            Path stem = workDir.resolve("stem-" + (i++) + "-" + entry.getKey() + "." + ext);
            runFfmpeg(built, "[" + entry.getValue() + "]", ctx, format, stem, built.estimatedDurationSeconds());
            stemFiles.add(stem);
        }
        Path zip = workDir.resolve("stems.zip");
        try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            for (Path stem : stemFiles) {
                zos.putNextEntry(new java.util.zip.ZipEntry(stem.getFileName().toString()));
                Files.copy(stem, zos);
                zos.closeEntry();
            }
        }
        return zip;
    }

    private Map<UUID, Path> downloadAssets(ProjectGraph graph, Path workDir) {
        Map<UUID, String> keys = new LinkedHashMap<>();
        graph.tracks().forEach(t -> t.clips().forEach(c -> keys.putIfAbsent(c.sourceAssetId(), c.assetObjectKey())));
        Map<UUID, Path> files = new LinkedHashMap<>();
        keys.forEach((assetId, key) -> {
            String suffix = key.contains(".") ? key.substring(key.lastIndexOf('.')) : "";
            Path dest = workDir.resolve("src-" + assetId + suffix);
            storage.download(key, dest);
            files.put(assetId, dest);
        });
        return files;
    }

    private double safeDuration(Path file, double fallback) {
        try {
            return probeService.probe(file).durationSeconds();
        } catch (Exception e) {
            return Math.max(fallback, 0);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }
}
