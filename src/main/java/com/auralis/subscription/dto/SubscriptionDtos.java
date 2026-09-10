package com.auralis.subscription.dto;

import com.auralis.subscription.QuotaService.UsageSnapshot;
import com.auralis.subscription.domain.Plan;
import com.auralis.subscription.domain.PlanEntitlements;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.domain.Subscription;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class SubscriptionDtos {

    private SubscriptionDtos() {}

    public record EntitlementsView(
            int maxProjects,
            int maxCollaboratorsPerProject,
            int monthlyRenderMinutes,
            long maxStorageBytes,
            long maxUploadBytes,
            List<String> allowedExportFormats,
            boolean allowStemExport,
            int maxExportBitrateKbps,
            int historyRetentionDays) {

        public static EntitlementsView of(PlanEntitlements e) {
            return new EntitlementsView(
                    e.getMaxProjects(), e.getMaxCollaboratorsPerProject(), e.getMonthlyRenderMinutes(),
                    e.getMaxStorageBytes(), e.getMaxUploadBytes(), List.copyOf(e.exportFormats()),
                    e.isAllowStemExport(), e.getMaxExportBitrateKbps(), e.getHistoryRetentionDays());
        }
    }

    public record PlanResponse(String tier, String name, String description,
                               long priceCents, String currency, EntitlementsView entitlements) {
        public static PlanResponse of(Plan p) {
            return new PlanResponse(p.getTier().name(), p.getName(), p.getDescription(),
                    p.getPriceCents(), p.getCurrency(), EntitlementsView.of(p.getEntitlements()));
        }
    }

    public record UsageView(long renderSecondsUsed, long renderSecondsLimit,
                            long storageBytesUsed, long storageBytesLimit) {
        public static UsageView of(UsageSnapshot s) {
            return new UsageView(s.renderSecondsUsed(), s.renderSecondsLimit(),
                    s.storageBytesUsed(), s.storageBytesLimit());
        }
    }

    public record SubscriptionResponse(String tier, String status, Instant currentPeriodEnd,
                                       boolean cancelAtPeriodEnd, EntitlementsView entitlements, UsageView usage) {
        public static SubscriptionResponse of(Subscription s, PlanEntitlements e, UsageSnapshot usage) {
            return new SubscriptionResponse(s.getTier().name(), s.getStatus().name(), s.getCurrentPeriodEnd(),
                    s.isCancelAtPeriodEnd(), EntitlementsView.of(e), UsageView.of(usage));
        }
    }

    public record ChangePlanRequest(@NotNull PlanTier tier) {}

    public record CancelRequest(boolean immediately) {}
}
