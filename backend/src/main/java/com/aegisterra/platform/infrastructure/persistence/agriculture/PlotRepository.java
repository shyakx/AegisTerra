package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlotRepository extends JpaRepository<PlotEntity, UUID> {
    List<PlotEntity> findByFarmIdAndDeletedFalse(UUID farmId);
}
