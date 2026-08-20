package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidenceEntity, UUID> {
    List<ClaimEvidenceEntity> findByClaimIdAndDeletedFalseOrderByCreatedAtDesc(UUID claimId);

    Optional<ClaimEvidenceEntity> findByIdAndDeletedFalse(UUID id);
}
