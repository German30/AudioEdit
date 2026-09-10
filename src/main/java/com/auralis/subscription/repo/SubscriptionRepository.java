package com.auralis.subscription.repo;

import com.auralis.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);

    boolean existsByUserId(UUID userId);
}
