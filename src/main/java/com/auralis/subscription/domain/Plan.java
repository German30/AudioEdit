package com.auralis.subscription.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A purchasable subscription plan. Rows are seeded by Flyway; the admin API can
 * tweak pricing / entitlements at runtime.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "plans", indexes = @Index(name = "ux_plans_tier", columnList = "tier", unique = true))
public class Plan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanTier tier;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    /** Monthly price in minor units (cents). {@code 0} for the free tier. */
    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Embedded
    private PlanEntitlements entitlements = new PlanEntitlements();
}
