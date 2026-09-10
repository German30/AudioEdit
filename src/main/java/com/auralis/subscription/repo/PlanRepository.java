package com.auralis.subscription.repo;

import com.auralis.subscription.domain.Plan;
import com.auralis.subscription.domain.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByTier(PlanTier tier);

    List<Plan> findAllByActiveTrueOrderBySortOrderAsc();
}
