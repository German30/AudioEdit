package com.auralis.subscription.web;

import com.auralis.subscription.SubscriptionService;
import com.auralis.subscription.domain.SubscriptionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Placeholder for the payment provider's webhook. A real integration would
 * verify the signature header and map the provider's event catalogue; here we
 * accept a normalized shape so the rest of the platform can be exercised.
 *
 * <pre>{ "externalSubscriptionId": "...", "status": "ACTIVE|PAST_DUE|CANCELED", "periodEndEpochSeconds": 173... }</pre>
 */
@Slf4j
@Tag(name = "Billing")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingWebhookController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Payment-provider webhook (stub): apply an external subscription status change")
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                        @RequestBody Map<String, Object> event) {
        // TODO: verify `signature` against the provider secret before trusting the payload.
        String externalId = String.valueOf(event.get("externalSubscriptionId"));
        SubscriptionStatus status = SubscriptionStatus.valueOf(
                String.valueOf(event.getOrDefault("status", "ACTIVE")).toUpperCase());
        Instant periodEnd = event.get("periodEndEpochSeconds") == null ? null
                : Instant.ofEpochSecond(((Number) event.get("periodEndEpochSeconds")).longValue());

        subscriptionService.applyExternalStatus(externalId, status, periodEnd);
        return ResponseEntity.accepted().build();
    }
}
