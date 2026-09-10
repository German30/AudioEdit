package com.auralis.auth.web;

import com.auralis.auth.domain.User;
import com.auralis.auth.dto.AuthDtos.UpdateProfileRequest;
import com.auralis.auth.dto.AuthDtos.UserResponse;
import com.auralis.auth.repo.UserRepository;
import com.auralis.auth.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping
    public UserResponse me() {
        return UserResponse.of(currentUser.entity());
    }

    @Operation(summary = "Update the authenticated user's display name / avatar")
    @PatchMapping
    @Transactional
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest body) {
        User u = currentUser.entity();
        if (body.displayName() != null) u.setDisplayName(body.displayName());
        if (body.avatarUrl() != null) u.setAvatarUrl(body.avatarUrl());
        return UserResponse.of(userRepository.save(u));
    }
}
