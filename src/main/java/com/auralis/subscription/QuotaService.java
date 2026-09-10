package com.auralis.subscription;

import com.auralis.common.error.QuotaExceededException;
import com.auralis.subscription.domain.PlanEntitlements;
import com.auralis.subscription.domain.UsageCounter;
import com.auralis.subscription.repo.UsageCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforces plan limits. Other modules pass in the counts they own (project
 * count, collaborator count, ...); this service owns the persistent usage
 * counters (monthly render seconds, stored bytes).
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final SubscriptionService subscriptionService;
    private final UsageCounterRepository usageCounterRepository;

    // ---- pre-flight assertions -----------------------------------------

    @Transactional(readOnly = true)
    public void assertCanCreateProject(UUID userId, long currentProjectCount) {
        PlanEntitlements e = entitlements(userId);
        if (!e.isUnlimitedProjects() && currentProjectCount >= e.getMaxProjects()) {
            throw new QuotaExceededException("projects",
                    "plan allows %d project(s)".formatted(e.getMaxProjects()));
        }
    }

    @Transactional(readOnly = true)
    public void assertCollaboratorLimit(UUID ownerUserId, long collaboratorCountAfterAdd) {
        PlanEntitlements e = entitlements(ownerUserId);
        int max = e.getMaxCollaboratorsPerProject();
        if (max >= 0 && collaboratorCountAfterAdd > max) {
            throw new QuotaExceededException("collaborators",
                    "plan allows %d collaborator(s) per project".formatted(max));
        }
    }

    @Transactional(readOnly = true)
    public void assertUploadAllowed(UUID userId, long fileBytes) {
        PlanEntitlements e = entitlements(userId);
        if (e.getMaxUploadBytes() >= 0 && fileBytes > e.getMaxUploadBytes()) {
            throw new QuotaExceededException("upload_size",
                    "plan allows uploads up to %d bytes".formatted(e.getMaxUploadBytes()));
        }
        assertStorageAvailable(userId, fileBytes);
    }

    @Transactional(readOnly = true)
    public void assertStorageAvailable(UUID userId, long additionalBytes) {
        PlanEntitlements e = entitlements(userId);
        if (e.isUnlimitedStorage()) return;
        long used = usageCounterRepository.findByUserId(userId).map(UsageCounter::getStorageBytesUsed).orElse(0L);
        if (used + additionalBytes > e.getMaxStorageBytes()) {
            throw new QuotaExceededException("storage",
                    "%d of %d bytes already used".formatted(used, e.getMaxStorageBytes()));
        }
    }

    @Transactional(readOnly = true)
    public void assertRenderAllowed(UUID userId, RenderRequestLimits req) {
        PlanEntitlements e = entitlements(userId);

        if (!e.allowsExportFormat(req.format())) {
            throw new QuotaExceededException("export_format",
                    "plan allows formats %s".formatted(e.exportFormats()));
        }
        if (req.stems() && !e.isAllowStemExport()) {
            throw new QuotaExceededException("stem_export", "stem export requires a higher plan");
        }
        if (e.getMaxExportBitrateKbps() > 0 && req.bitrateKbps() > e.getMaxExportBitrateKbps()) {
            throw new QuotaExceededException("export_bitrate",
                    "plan caps export bitrate at %d kbps".formatted(e.getMaxExportBitrateKbps()));
        }
        if (!e.isUnlimitedRenderMinutes()) {
            long usedSeconds = currentRenderSeconds(userId);
            long capSeconds = (long) e.getMonthlyRenderMinutes() * 60;
            if (usedSeconds + Math.max(req.estimatedSeconds(), 0) > capSeconds) {
                throw new QuotaExceededException("render_minutes",
                        "%d of %d render-minutes used this month"
                                .formatted(usedSeconds / 60, e.getMonthlyRenderMinutes()));
            }
        }
    }

    // ---- usage bookkeeping -------------------------------------------

    @Transactional
    public void recordRenderUsage(UUID userId, long seconds) {
        UsageCounter counter = lockedCounter(userId);
        counter.rollPeriodIfNeeded();
        counter.setRenderSecondsUsed(counter.getRenderSecondsUsed() + Math.max(seconds, 0));
    }

    @Transactional
    public void addStorageUsage(UUID userId, long deltaBytes) {
        UsageCounter counter = lockedCounter(userId);
        long next = counter.getStorageBytesUsed() + deltaBytes;
        counter.setStorageBytesUsed(Math.max(next, 0));
    }

    @Transactional(readOnly = true)
    public UsageSnapshot usage(UUID userId) {
        PlanEntitlements e = entitlements(userId);
        UsageCounter c = usageCounterRepository.findByUserId(userId).orElseGet(UsageCounter::new);
        long renderSeconds = c.getPeriodMonth() != null && c.getPeriodMonth().equals(UsageCounter.firstOfCurrentMonth())
                ? c.getRenderSecondsUsed() : 0;
        return new UsageSnapshot(
                renderSeconds,
                e.isUnlimitedRenderMinutes() ? -1 : (long) e.getMonthlyRenderMinutes() * 60,
                c.getStorageBytesUsed(),
                e.getMaxStorageBytes());
    }

    // ---- helpers ---------------------------------------------------

    private PlanEntitlements entitlements(UUID userId) {
        return subscriptionService.entitlementsFor(userId);
    }

    private long currentRenderSeconds(UUID userId) {
        return usageCounterRepository.findByUserId(userId)
                .filter(c -> UsageCounter.firstOfCurrentMonth().equals(c.getPeriodMonth()))
                .map(UsageCounter::getRenderSecondsUsed)
                .orElse(0L);
    }

    private UsageCounter lockedCounter(UUID userId) {
        return usageCounterRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            UsageCounter c = new UsageCounter();
            c.setUserId(userId);
            return usageCounterRepository.save(c);
        });
    }

    /** Inputs the render module needs checked before queuing a job. */
    public record RenderRequestLimits(long estimatedSeconds, String format, int bitrateKbps, boolean stems) {}

    public record UsageSnapshot(long renderSecondsUsed, long renderSecondsLimit,
                                long storageBytesUsed, long storageBytesLimit) {}
}
