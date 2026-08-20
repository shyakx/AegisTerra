package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoveragePackageRepository extends JpaRepository<CoveragePackageEntity, UUID> {
    List<CoveragePackageEntity> findByProductIdAndDeletedFalseOrderByNameAsc(UUID productId);
    Optional<CoveragePackageEntity> findByIdAndDeletedFalse(UUID id);
    Optional<CoveragePackageEntity> findByProductIdAndCodeAndDeletedFalse(UUID productId, String code);
}
