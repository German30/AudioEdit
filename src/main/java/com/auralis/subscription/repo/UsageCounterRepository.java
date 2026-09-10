package com.auralis.subscription.repo;

import com.auralis.subscription.domain.UsageCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {

    Optional<UsageCounter> findByUserId(UUID userId);

    /** Pessimistic lock so concurrent renders/uploads can't race the counter. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsageCounter u where u.userId = :userId")
    Optional<UsageCounter> findByUserIdForUpdate(@Param("userId") UUID userId);
}
