package com.auralis.auth.event;

import java.util.UUID;

/**
 * Published the first time a user record is created (via OAuth2 or dev-login).
 * The subscription module listens for this to provision a default Free plan.
 */
public record UserRegisteredEvent(UUID userId, String email) {
}
