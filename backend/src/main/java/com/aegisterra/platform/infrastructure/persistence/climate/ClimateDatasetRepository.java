package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateDatasetRepository extends JpaRepository<ClimateDatasetEntity, UUID> {
    Page<ClimateDatasetEntity> findByDeletedFalse(Pageable pageable);

    Page<ClimateDatasetEntity> findByStatusAndDeletedFalse(String status, Pageable pageable);

    Optional<ClimateDatasetEntity> findByIdAndDeletedFalse(UUID id);
}
