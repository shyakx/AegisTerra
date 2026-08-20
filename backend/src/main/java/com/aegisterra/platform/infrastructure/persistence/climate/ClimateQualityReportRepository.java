package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateQualityReportRepository extends JpaRepository<ClimateQualityReportEntity, UUID> {
    Page<ClimateQualityReportEntity> findAllByOrderByGeneratedAtDesc(Pageable pageable);

    Optional<ClimateQualityReportEntity> findFirstByOrderByGeneratedAtDesc();
}
