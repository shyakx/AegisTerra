package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<PayoutEntity, UUID> {
    Optional<PayoutEntity> findByClaimIdAndDeletedFalse(UUID claimId);
}
