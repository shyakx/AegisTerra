package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimAssessmentRepository extends JpaRepository<ClaimAssessmentEntity, UUID> {
    List<ClaimAssessmentEntity> findByClaimIdAndDeletedFalse(UUID claimId);
}
