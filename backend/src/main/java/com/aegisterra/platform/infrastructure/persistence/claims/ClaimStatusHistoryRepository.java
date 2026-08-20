package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistoryEntity, UUID> {
    List<ClaimStatusHistoryEntity> findByClaimIdAndDeletedFalseOrderByOccurredAtAsc(UUID claimId);
}
