package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageLimitRepository extends JpaRepository<CoverageLimitEntity, UUID> {
    List<CoverageLimitEntity> findByCoveragePackageIdAndDeletedFalse(UUID coveragePackageId);
}
