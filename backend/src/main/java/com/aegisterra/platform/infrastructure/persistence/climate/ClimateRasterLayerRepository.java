package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateRasterLayerRepository extends JpaRepository<ClimateRasterLayerEntity, UUID> {
    Page<ClimateRasterLayerEntity> findByDeletedFalse(Pageable pageable);
}
