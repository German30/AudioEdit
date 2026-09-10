package com.auralis.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The concrete limits a {@link Plan} grants. A value of {@code -1} means
 * "unlimited". Stored inline on the {@code plans} table.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlanEntitlements {

    @Column(name = "max_projects", nullable = false)
    private int maxProjects;

    @Column(name = "max_collaborators", nullable = false)
    private int maxCollaboratorsPerProject;

    @Column(name = "monthly_render_minutes", nullable = false)
    private int monthlyRenderMinutes;

    @Column(name = "max_storage_bytes", nullable = false)
    private long maxStorageBytes;

    @Column(name = "max_upload_bytes", nullable = false)
    private long maxUploadBytes;

    /** Comma-separated lowercase container formats, e.g. {@code "mp3,wav,flac"}. */
    @Column(name = "allowed_export_formats", nullable = false, length = 255)
    private String allowedExportFormats = "mp3";

    @Column(name = "allow_stem_export", nullable = false)
    private boolean allowStemExport;

    @Column(name = "max_export_bitrate_kbps", nullable = false)
    private int maxExportBitrateKbps;

    @Column(name = "history_retention_days", nullable = false)
    private int historyRetentionDays;

    public boolean isUnlimitedProjects() {
        return maxProjects < 0;
    }

    public boolean isUnlimitedRenderMinutes() {
        return monthlyRenderMinutes < 0;
    }

    public boolean isUnlimitedStorage() {
        return maxStorageBytes < 0;
    }

    public Set<String> exportFormats() {
        Set<String> out = new LinkedHashSet<>();
        Arrays.stream(allowedExportFormats.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .forEach(out::add);
        return out;
    }

    public boolean allowsExportFormat(String format) {
        return format != null && exportFormats().contains(format.toLowerCase(java.util.Locale.ROOT));
    }
}
