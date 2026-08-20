package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VillageRepository extends JpaRepository<VillageEntity, UUID> {
    List<VillageEntity> findByCellIdAndDeletedFalse(UUID cellId);
}
