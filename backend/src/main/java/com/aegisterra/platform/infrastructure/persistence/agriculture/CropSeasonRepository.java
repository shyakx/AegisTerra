package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropSeasonRepository extends JpaRepository<CropSeasonEntity, UUID> {
    List<CropSeasonEntity> findByFarmIdAndDeletedFalse(UUID farmId);

    List<CropSeasonEntity> findBySeasonIdAndDeletedFalse(UUID seasonId);
}
