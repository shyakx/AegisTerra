package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.SystemRoleIds;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the linked farmer subject for FARMER-role users and enforces ownership.
 */
@Service
public class FarmerSubjectScope {

    private final FarmerRepository farmerRepository;

    public FarmerSubjectScope(FarmerRepository farmerRepository) {
        this.farmerRepository = farmerRepository;
    }

    public boolean isSubjectScoped(AegisUserPrincipal actor) {
        if (actor == null || actor.roles() == null) {
            return false;
        }
        if (actor.roles().contains(SystemRoleIds.SYSTEM_ADMIN_CODE)) {
            return false;
        }
        return actor.roles().contains("FARMER");
    }

    @Transactional(readOnly = true)
    public Optional<UUID> subjectFarmerId(AegisUserPrincipal actor) {
        if (!isSubjectScoped(actor)) {
            return Optional.empty();
        }
        return farmerRepository.findByUserIdAndDeletedFalse(actor.id()).map(FarmerEntity::getId);
    }

    @Transactional(readOnly = true)
    public UUID requireSubjectFarmerId(AegisUserPrincipal actor) {
        return subjectFarmerId(actor)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No farmer profile is linked to this account"
            ));
    }

    public void assertOwnsFarmer(AegisUserPrincipal actor, UUID farmerId) {
        subjectFarmerId(actor).ifPresent(owned -> {
            if (!owned.equals(farmerId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Farmer not found");
            }
        });
    }

    public UUID forceFarmerId(AegisUserPrincipal actor, UUID requestedFarmerId) {
        Optional<UUID> owned = subjectFarmerId(actor);
        if (owned.isEmpty()) {
            return requestedFarmerId;
        }
        if (requestedFarmerId != null && !owned.get().equals(requestedFarmerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return owned.get();
    }
}
