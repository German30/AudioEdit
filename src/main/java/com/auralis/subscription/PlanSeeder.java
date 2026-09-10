package com.auralis.subscription;

import com.auralis.subscription.domain.Plan;
import com.auralis.subscription.domain.PlanEntitlements;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.repo.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the three plan rows exist. Idempotent: only inserts tiers that are
 * missing, so it is safe to run on every boot and alongside Flyway.
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class PlanSeeder implements ApplicationRunner {

    private final PlanRepository planRepository;

    private static final long GB = 1024L * 1024 * 1024;
    private static final long MB = 1024L * 1024;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(PlanTier.FREE, "Free", "Get started: a few projects and light exports.", 0, 0,
                ent(3, 1, 30, 500 * MB, 50 * MB, "mp3", false, 192, 14));
        seed(PlanTier.PRO, "Pro", "For serious hobbyists and freelancers.", 1200, 1,
                ent(50, 5, 600, 25 * GB, 300 * MB, "mp3,wav,flac", true, 320, 90));
        seed(PlanTier.STUDIO, "Studio", "Studios and teams: unlimited projects, lossless everything.", 4900, 2,
                ent(-1, 25, 6000, 250 * GB, 2 * GB, "mp3,wav,flac,aac,ogg", true, 320, 365));
    }

    private void seed(PlanTier tier, String name, String description, long priceCents, int sortOrder,
                      PlanEntitlements entitlements) {
        if (planRepository.findByTier(tier).isPresent()) {
            return;
        }
        Plan plan = new Plan();
        plan.setTier(tier);
        plan.setName(name);
        plan.setDescription(description);
        plan.setPriceCents(priceCents);
        plan.setCurrency("USD");
        plan.setActive(true);
        plan.setSortOrder(sortOrder);
        plan.setEntitlements(entitlements);
        planRepository.save(plan);
        log.info("Seeded plan {}", tier);
    }

    private PlanEntitlements ent(int maxProjects, int maxCollaborators, int renderMinutes, long storageBytes,
                                 long uploadBytes, String formats, boolean stems, int bitrateKbps, int retentionDays) {
        PlanEntitlements e = new PlanEntitlements();
        e.setMaxProjects(maxProjects);
        e.setMaxCollaboratorsPerProject(maxCollaborators);
        e.setMonthlyRenderMinutes(renderMinutes);
        e.setMaxStorageBytes(storageBytes);
        e.setMaxUploadBytes(uploadBytes);
        e.setAllowedExportFormats(formats);
        e.setAllowStemExport(stems);
        e.setMaxExportBitrateKbps(bitrateKbps);
        e.setHistoryRetentionDays(retentionDays);
        return e;
    }
}
