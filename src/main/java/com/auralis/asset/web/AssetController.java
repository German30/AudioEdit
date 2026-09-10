package com.auralis.asset.web;

import com.auralis.asset.AssetService;
import com.auralis.asset.dto.AssetDtos.AssetResponse;
import com.auralis.asset.dto.AssetDtos.DownloadUrlResponse;
import com.auralis.asset.dto.AssetDtos.InitiateUploadRequest;
import com.auralis.asset.dto.AssetDtos.InitiateUploadResponse;
import com.auralis.auth.security.CurrentUser;
import com.auralis.common.web.PageResponse;
import com.auralis.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Upload lifecycle: {@code POST /uploads} returns a presigned URL, the client
 * PUTs the bytes straight to storage, then {@code POST /{id}/complete} kicks off
 * probing + waveform generation.
 */
@Tag(name = "Assets")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final CurrentUser currentUser;

    @Operation(summary = "Reserve an asset and get a presigned upload URL")
    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public InitiateUploadResponse initiateUpload(@Valid @RequestBody InitiateUploadRequest body) {
        return InitiateUploadResponse.of(assetService.initiateUpload(
                currentUser.id(), body.filename(), body.contentType(), body.sizeBytes()));
    }

    @Operation(summary = "Mark the upload complete; starts async analysis")
    @PostMapping("/{assetId}/complete")
    public AssetResponse complete(@PathVariable UUID assetId) {
        return AssetResponse.of(assetService.completeUpload(currentUser.id(), assetId));
    }

    @Operation(summary = "List the caller's assets")
    @GetMapping
    public PageResponse<AssetResponse> list(Pageable pageable) {
        return PageResponse.from(assetService.list(currentUser.id(), pageable), AssetResponse::of);
    }

    @Operation(summary = "Get a single asset with its analysis metadata")
    @GetMapping("/{assetId}")
    public AssetResponse get(@PathVariable UUID assetId) {
        return AssetResponse.of(assetService.get(currentUser.id(), assetId));
    }

    @Operation(summary = "Get a short-lived download URL for the original file")
    @GetMapping("/{assetId}/download-url")
    public DownloadUrlResponse downloadUrl(@PathVariable UUID assetId) {
        StorageService.PresignedDownload d = assetService.download(currentUser.id(), assetId);
        return new DownloadUrlResponse(d.url(), d.expiresAt());
    }

    @Operation(summary = "Delete an asset (rejected if still referenced by a project)")
    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID assetId) {
        assetService.delete(currentUser.id(), assetId);
    }
}
