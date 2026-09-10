package com.auralis.subscription;

import com.auralis.auth.event.UserRegisteredEvent;
import com.auralis.common.error.ApiException;
import com.auralis.subscription.domain.Plan;
import com.auralis.subscription.domain.PlanEntitlements;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.domain.Subscription;
import com.auralis.subscription.domain.SubscriptionStatus;
import com.auralis.subscription.domain.UsageCounter;
import com.auralis.subscription.repo.PlanRepository;
import com.auralis.subscription.repo.SubscriptionRepository;
import com.auralis.subscription.repo.UsageCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Plan catalogue + per-user subscription lifecycle. Payment-provider
 * integration is represented by {@link #applyExternalStatus} / the billing
 * webhook; plan changes here take effect immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageCounterRepository usageCounterRepository;

    // ---- catalogue --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Plan> listPlans() {
        return planRepository.findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Plan getPlan(PlanTier tier) {
        return planRepository.findByTier(tier)
                .orElseThrow(() -> ApiException.notFound("Plan " + tier));
    }

    // ---- provisioning ---------------------------------------------------

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        provisionDefault(event.userId());
    }

    @Transactional
    public Subscription provisionDefault(UUID userId) {
        return subscriptionRepository.findByUserId(userId).orElseGet(() -> {
            Subscription sub = new Subscription();
            sub.setUserId(userId);
            sub.setTier(PlanTier.FREE);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub = subscriptionRepository.save(sub);

            if (!usageCounterRepository.findByUserId(userId).isPresent()) {
                UsageCounter counter = new UsageCounter();
                counter.setUserId(userId);
                usageCounterRepository.save(counter);
            }
            log.debug("Provisioned FREE subscription for user {}", userId);
            return sub;
        });
    }

    // ---- reads ----------------------------------------------------------

    @Transactional
    public Subscription getForUser(UUID userId) {
        return subscriptionRepository.findByUserId(userId).orElseGet(() -> provisionDefault(userId));
    }

    @Transactional
    public PlanEntitlements entitlementsFor(UUID userId) {
        Subscription sub = getForUser(userId);
        PlanTier effectiveTier = sub.getStatus() == SubscriptionStatus.CANCELED
                || sub.getStatus() == SubscriptionStatus.PAST_DUE
                ? PlanTier.FREE : sub.getTier();
        return getPlan(effectiveTier).getEntitlements();
    }

    // ---- mutations ----------------------------------------------------

    @Transactional
    public Subscription changePlan(UUID userId, PlanTier targetTier) {
        Plan target = getPlan(targetTier); // validates existence
        Subscription sub = getForUser(userId);
        if (sub.getTier() == targetTier && sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return sub;
        }
        sub.setTier(targetTier);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelAtPeriodEnd(false);
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(target.getTier() == PlanTier.FREE
                ? null : Instant.now().plus(30, ChronoUnit.DAYS));
        log.info("User {} changed plan to {}", userId, targetTier);
        return sub;
    }

    @Transactional
    public Subscription cancel(UUID userId, boolean immediately) {
        Subscription sub = getForUser(userId);
        if (sub.getTier() == PlanTier.FREE) {
            return sub;
        }
        if (immediately) {
            sub.setTier(PlanTier.FREE);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCancelAtPeriodEnd(false);
            sub.setCurrentPeriodEnd(null);
        } else {
            sub.setCancelAtPeriodEnd(true);
        }
        return sub;
    }

    @Transactional
    public Subscription reactivate(UUID userId) {
        Subscription sub = getForUser(userId);
        sub.setCancelAtPeriodEnd(false);
        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
        }
        return sub;
    }

    /** Entry point for billing-provider webhook events. */
    @Transactional
    public void applyExternalStatus(String externalSubscriptionId, SubscriptionStatus status,
                                    Instant periodEnd) {
        subscriptionRepository.findByExternalSubscriptionId(externalSubscriptionId).ifPresentOrElse(sub -> {
            sub.setStatus(status);
            if (periodEnd != null) sub.setCurrentPeriodEnd(periodEnd);
            if (status == SubscriptionStatus.CANCELED) {
                sub.setTier(PlanTier.FREE);
            }
        }, () -> log.warn("Webhook for unknown subscription {}", externalSubscriptionId));
    }

    /** Downgrade subscriptions whose paid period has lapsed. Runs daily. */
    @Transactional
    public int expireLapsedSubscriptions() {
        int changed = 0;
        for (Subscription sub : subscriptionRepository.findAll()) {
            if (sub.getTier() == PlanTier.FREE || sub.getCurrentPeriodEnd() == null) continue;
            if (sub.getCurrentPeriodEnd().isBefore(Instant.now())) {
                if (sub.isCancelAtPeriodEnd()) {
                    sub.setTier(PlanTier.FREE);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    sub.setCurrentPeriodEnd(null);
                    sub.setCancelAtPeriodEnd(false);
                } else {
                    sub.setStatus(SubscriptionStatus.PAST_DUE);
                }
                changed++;
            }
        }
        return changed;
    }
}
