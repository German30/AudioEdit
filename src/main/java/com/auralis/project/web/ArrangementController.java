package com.auralis.project.web;

import com.auralis.auth.security.CurrentUser;
import com.auralis.project.ClipService;
import com.auralis.project.EffectService;
import com.auralis.project.TrackService;
import com.auralis.project.dto.ProjectDtos.ClipResponse;
import com.auralis.project.dto.ProjectDtos.CreateClipRequest;
import com.auralis.project.dto.ProjectDtos.CreateEffectRequest;
import com.auralis.project.dto.ProjectDtos.CreateTrackRequest;
import com.auralis.project.dto.ProjectDtos.EffectResponse;
import com.auralis.project.dto.ProjectDtos.ReorderTracksRequest;
import com.auralis.project.dto.ProjectDtos.SplitClipRequest;
import com.auralis.project.dto.ProjectDtos.TrackResponse;
import com.auralis.project.dto.ProjectDtos.UpdateClipRequest;
import com.auralis.project.dto.ProjectDtos.UpdateEffectRequest;
import com.auralis.project.dto.ProjectDtos.UpdateTrackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tracks, clips and effects for a project. All paths are nested under the
 * project so access control is applied per project.
 */
@Tag(name = "Arrangement")
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
public class ArrangementController {

    private final TrackService trackService;
    private final ClipService clipService;
    private final EffectService effectService;
    private final CurrentUser currentUser;

    // ---- tracks ----

    @Operation(summary = "List tracks")
    @GetMapping("/tracks")
    public List<TrackResponse> listTracks(@PathVariable UUID projectId) {
        return trackService.list(currentUser.id(), projectId).stream().map(TrackResponse::of).toList();
    }

    @Operation(summary = "Add a track")
    @PostMapping("/tracks")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackResponse addTrack(@PathVariable UUID projectId, @Valid @RequestBody CreateTrackRequest body) {
        return TrackResponse.of(trackService.add(currentUser.id(), projectId, body.name(), body.color()));
    }

    @Operation(summary = "Update a track (gain, pan, mute, solo, ...)")
    @PatchMapping("/tracks/{trackId}")
    public TrackResponse updateTrack(@PathVariable UUID projectId, @PathVariable UUID trackId,
                                     @RequestBody UpdateTrackRequest body) {
        return TrackResponse.of(trackService.update(currentUser.id(), projectId, trackId,
                new TrackService.TrackPatch(body.name(), body.gainDb(), body.pan(), body.muted(),
                        body.soloed(), body.color())));
    }

    @Operation(summary = "Delete a track and its clips")
    @DeleteMapping("/tracks/{trackId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrack(@PathVariable UUID projectId, @PathVariable UUID trackId) {
        trackService.delete(currentUser.id(), projectId, trackId);
    }

    @Operation(summary = "Reorder all tracks")
    @PutMapping("/tracks/order")
    public List<TrackResponse> reorderTracks(@PathVariable UUID projectId,
                                             @Valid @RequestBody ReorderTracksRequest body) {
        return trackService.reorder(currentUser.id(), projectId, body.trackIds()).stream()
                .map(TrackResponse::of).toList();
    }

    // ---- clips ----

    @Operation(summary = "List clips across all tracks")
    @GetMapping("/clips")
    public List<ClipResponse> listClips(@PathVariable UUID projectId) {
        return clipService.list(currentUser.id(), projectId).stream().map(ClipResponse::of).toList();
    }

    @Operation(summary = "Place an asset on a track as a clip")
    @PostMapping("/clips")
    @ResponseStatus(HttpStatus.CREATED)
    public ClipResponse addClip(@PathVariable UUID projectId, @Valid @RequestBody CreateClipRequest body) {
        return ClipResponse.of(clipService.add(currentUser.id(), projectId, body.trackId(), body.sourceAssetId(),
                new ClipService.NewClip(body.name(), body.timelineStartSeconds(), body.sourceOffsetSeconds(),
                        body.durationSeconds(), body.gainDb(), body.fadeInSeconds(), body.fadeOutSeconds(),
                        body.playbackRate(), body.reversed())));
    }

    @Operation(summary = "Update a clip (position, trim, gain, fades, speed, reverse)")
    @PatchMapping("/clips/{clipId}")
    public ClipResponse updateClip(@PathVariable UUID projectId, @PathVariable UUID clipId,
                                   @RequestBody UpdateClipRequest body) {
        return ClipResponse.of(clipService.update(currentUser.id(), projectId, clipId,
                new ClipService.ClipPatch(body.trackId(), body.name(), body.timelineStartSeconds(),
                        body.sourceOffsetSeconds(), body.durationSeconds(), body.gainDb(), body.fadeInSeconds(),
                        body.fadeOutSeconds(), body.playbackRate(), body.reversed())));
    }

    @Operation(summary = "Split a clip at a timeline position")
    @PostMapping("/clips/{clipId}/split")
    public List<ClipResponse> splitClip(@PathVariable UUID projectId, @PathVariable UUID clipId,
                                        @Valid @RequestBody SplitClipRequest body) {
        return clipService.split(currentUser.id(), projectId, clipId, body.atTimelineSeconds()).stream()
                .map(ClipResponse::of).toList();
    }

    @Operation(summary = "Delete a clip")
    @DeleteMapping("/clips/{clipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClip(@PathVariable UUID projectId, @PathVariable UUID clipId) {
        clipService.delete(currentUser.id(), projectId, clipId);
    }

    // ---- effects ----

    @Operation(summary = "List every effect in the project")
    @GetMapping("/effects")
    public List<EffectResponse> listEffects(@PathVariable UUID projectId) {
        return effectService.list(currentUser.id(), projectId).stream().map(EffectResponse::of).toList();
    }

    @Operation(summary = "Add an effect to a clip, track or the master bus")
    @PostMapping("/effects")
    @ResponseStatus(HttpStatus.CREATED)
    public EffectResponse addEffect(@PathVariable UUID projectId, @Valid @RequestBody CreateEffectRequest body) {
        return EffectResponse.of(effectService.add(currentUser.id(), projectId,
                new EffectService.NewEffect(body.scope(), body.clipId(), body.trackId(), body.type(),
                        body.enabled(), body.params())));
    }

    @Operation(summary = "Update an effect (enable, reorder, params)")
    @PatchMapping("/effects/{effectId}")
    public EffectResponse updateEffect(@PathVariable UUID projectId, @PathVariable UUID effectId,
                                       @RequestBody UpdateEffectRequest body) {
        return EffectResponse.of(effectService.update(currentUser.id(), projectId, effectId,
                new EffectService.EffectPatch(body.enabled(), body.position(), body.params())));
    }

    @Operation(summary = "Delete an effect")
    @DeleteMapping("/effects/{effectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEffect(@PathVariable UUID projectId, @PathVariable UUID effectId) {
        effectService.delete(currentUser.id(), projectId, effectId);
    }
}
