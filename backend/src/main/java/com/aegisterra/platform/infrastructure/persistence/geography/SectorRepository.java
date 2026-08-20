package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<SectorEntity, UUID> {
    List<SectorEntity> findByDistrictIdAndDeletedFalse(UUID districtId);
}
