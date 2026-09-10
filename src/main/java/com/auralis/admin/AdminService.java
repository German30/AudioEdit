package com.auralis.admin;

import com.auralis.auth.domain.Role;
import com.auralis.auth.domain.User;
import com.auralis.auth.repo.UserRepository;
import com.auralis.common.error.ApiException;
import com.auralis.subscription.domain.Plan;
import com.auralis.subscription.domain.PlanTier;
import com.auralis.subscription.repo.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional
    public User updateUser(UUID id, Boolean enabled, Role role) {
        User user = getUser(id);
        if (enabled != null) user.setEnabled(enabled);
        if (role != null) user.setRole(role);
        return user;
    }

    @Transactional
    public Plan updatePlan(PlanTier tier, PlanUpdate update) {
        Plan plan = planRepository.findByTier(tier)
                .orElseThrow(() -> ApiException.notFound("Plan " + tier));
        if (update.name() != null) plan.setName(update.name());
        if (update.description() != null) plan.setDescription(update.description());
        if (update.priceCents() != null) plan.setPriceCents(update.priceCents());
        if (update.active() != null) plan.setActive(update.active());
        if (update.maxProjects() != null) plan.getEntitlements().setMaxProjects(update.maxProjects());
        if (update.maxCollaborators() != null)
            plan.getEntitlements().setMaxCollaboratorsPerProject(update.maxCollaborators());
        if (update.monthlyRenderMinutes() != null)
            plan.getEntitlements().setMonthlyRenderMinutes(update.monthlyRenderMinutes());
        if (update.maxStorageBytes() != null) plan.getEntitlements().setMaxStorageBytes(update.maxStorageBytes());
        if (update.maxUploadBytes() != null) plan.getEntitlements().setMaxUploadBytes(update.maxUploadBytes());
        if (update.allowedExportFormats() != null)
            plan.getEntitlements().setAllowedExportFormats(update.allowedExportFormats());
        if (update.allowStemExport() != null) plan.getEntitlements().setAllowStemExport(update.allowStemExport());
        if (update.maxExportBitrateKbps() != null)
            plan.getEntitlements().setMaxExportBitrateKbps(update.maxExportBitrateKbps());
        if (update.historyRetentionDays() != null)
            plan.getEntitlements().setHistoryRetentionDays(update.historyRetentionDays());
        return plan;
    }

    public record PlanUpdate(
            String name, String description, Long priceCents, Boolean active,
            Integer maxProjects, Integer maxCollaborators, Integer monthlyRenderMinutes,
            Long maxStorageBytes, Long maxUploadBytes, String allowedExportFormats,
            Boolean allowStemExport, Integer maxExportBitrateKbps, Integer historyRetentionDays) {}
}
