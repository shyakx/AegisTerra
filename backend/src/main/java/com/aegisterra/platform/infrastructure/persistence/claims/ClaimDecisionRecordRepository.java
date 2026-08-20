package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDecisionRecordRepository extends JpaRepository<ClaimDecisionRecordEntity, UUID> {
    List<ClaimDecisionRecordEntity> findByClaimIdAndDeletedFalseOrderByCreatedAtDesc(UUID claimId);
}
