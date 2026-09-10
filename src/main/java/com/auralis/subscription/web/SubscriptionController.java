package com.auralis.subscription.web;

import com.auralis.auth.security.CurrentUser;
import com.auralis.subscription.QuotaService;
import com.auralis.subscription.SubscriptionService;
import com.auralis.subscription.domain.Subscription;
import com.auralis.subscription.dto.SubscriptionDtos.CancelRequest;
import com.auralis.subscription.dto.SubscriptionDtos.ChangePlanRequest;
import com.auralis.subscription.dto.SubscriptionDtos.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Subscription")
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final QuotaService quotaService;
    private final CurrentUser currentUser;

    @Operation(summary = "The caller's current subscription, entitlements and usage")
    @GetMapping
    public SubscriptionResponse current() {
        return view(currentUser.id());
    }

    @Operation(summary = "Switch plan (takes effect immediately in this build)")
    @PostMapping("/change")
    public SubscriptionResponse change(@Valid @RequestBody ChangePlanRequest body) {
        UUID userId = currentUser.id();
        subscriptionService.changePlan(userId, body.tier());
        return view(userId);
    }

    @Operation(summary = "Cancel the paid plan (at period end unless immediately=true)")
    @PostMapping("/cancel")
    public SubscriptionResponse cancel(@RequestBody(required = false) CancelRequest body) {
        UUID userId = currentUser.id();
        subscriptionService.cancel(userId, body != null && body.immediately());
        return view(userId);
    }

    @Operation(summary = "Undo a pending cancellation")
    @PostMapping("/reactivate")
    public SubscriptionResponse reactivate() {
        UUID userId = currentUser.id();
        subscriptionService.reactivate(userId);
        return view(userId);
    }

    private SubscriptionResponse view(UUID userId) {
        Subscription sub = subscriptionService.getForUser(userId);
        return SubscriptionResponse.of(sub, subscriptionService.entitlementsFor(userId), quotaService.usage(userId));
    }
}
