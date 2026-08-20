package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitingPeriodRepository extends JpaRepository<WaitingPeriodEntity, UUID> {
    List<WaitingPeriodEntity> findByCoveragePackageIdAndDeletedFalse(UUID coveragePackageId);
    List<WaitingPeriodEntity> findByPolicyIdAndDeletedFalse(UUID policyId);
}
