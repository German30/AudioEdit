package com.auralis.subscription.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per user describing their current plan and billing period. Created
 * with tier {@code FREE} / status {@code ACTIVE} when the user registers.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "subscriptions", indexes = {
        @Index(name = "ux_subscriptions_user", columnList = "user_id", unique = true),
        @Index(name = "ix_subscriptions_external", columnList = "external_subscription_id")
})
public class Subscription extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanTier tier = PlanTier.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart = Instant.now();

    /** {@code null} for the free tier (no billing period). */
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "external_customer_id", length = 100)
    private String externalCustomerId;

    @Column(name = "external_subscription_id", length = 100)
    private String externalSubscriptionId;

    public boolean grantsPaidFeatures() {
        return tier != PlanTier.FREE
                && (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING);
    }
}
