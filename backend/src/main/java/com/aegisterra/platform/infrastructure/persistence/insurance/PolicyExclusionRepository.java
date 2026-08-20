package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyExclusionRepository extends JpaRepository<PolicyExclusionEntity, UUID> {
    List<PolicyExclusionEntity> findByCoveragePackageIdAndDeletedFalse(UUID coveragePackageId);
    List<PolicyExclusionEntity> findByPolicyIdAndDeletedFalse(UUID policyId);
}
