package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VegetationIndexRepository extends JpaRepository<VegetationIndexEntity, UUID> {
    List<VegetationIndexEntity> findByFarmIdAndIndexTypeOrderByObservedAtDesc(UUID farmId, String indexType);
}
