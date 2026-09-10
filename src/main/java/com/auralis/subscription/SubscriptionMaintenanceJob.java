package com.auralis.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionMaintenanceJob {

    private final SubscriptionService subscriptionService;

    /** Downgrade / mark past-due any paid subscriptions whose period has lapsed. */
    @Scheduled(cron = "0 0 * * * *")
    public void reconcile() {
        int changed = subscriptionService.expireLapsedSubscriptions();
        if (changed > 0) log.info("Reconciled {} lapsed subscription(s)", changed);
    }
}
