package com.auralis.subscription.web;

import com.auralis.subscription.SubscriptionService;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.dto.SubscriptionDtos.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Plans")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Public plan catalogue with pricing and entitlements")
    @GetMapping
    public List<PlanResponse> list() {
        return subscriptionService.listPlans().stream().map(PlanResponse::of).toList();
    }

    @Operation(summary = "Single plan by tier")
    @GetMapping("/{tier}")
    public PlanResponse get(@PathVariable PlanTier tier) {
        return PlanResponse.of(subscriptionService.getPlan(tier));
    }
}
