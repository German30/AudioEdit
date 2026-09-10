package com.auralis.admin.web;

import com.auralis.admin.AdminService;
import com.auralis.admin.AdminService.PlanUpdate;
import com.auralis.auth.domain.Role;
import com.auralis.auth.dto.AuthDtos.UserResponse;
import com.auralis.common.web.PageResponse;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.dto.SubscriptionDtos.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List all users")
    @GetMapping("/users")
    public PageResponse<UserResponse> users(Pageable pageable) {
        return PageResponse.from(adminService.listUsers(pageable), UserResponse::of);
    }

    @Operation(summary = "Get a user")
    @GetMapping("/users/{id}")
    public UserResponse user(@PathVariable UUID id) {
        return UserResponse.of(adminService.getUser(id));
    }

    @Operation(summary = "Enable/disable a user or change their account role")
    @PatchMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest body) {
        return UserResponse.of(adminService.updateUser(id, body.enabled(), body.role()));
    }

    @Operation(summary = "Update a plan's pricing / entitlements")
    @PutMapping("/plans/{tier}")
    public PlanResponse updatePlan(@PathVariable PlanTier tier, @RequestBody PlanUpdate body) {
        return PlanResponse.of(adminService.updatePlan(tier, body));
    }

    public record UpdateUserRequest(Boolean enabled, Role role) {}
}
