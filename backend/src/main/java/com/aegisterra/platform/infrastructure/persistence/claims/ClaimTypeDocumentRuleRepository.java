package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimTypeDocumentRuleRepository extends JpaRepository<ClaimTypeDocumentRuleEntity, UUID> {
    List<ClaimTypeDocumentRuleEntity> findByClaimTypeCodeAndDeletedFalse(String claimTypeCode);
}
