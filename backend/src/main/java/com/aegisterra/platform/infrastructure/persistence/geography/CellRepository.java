package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CellRepository extends JpaRepository<CellEntity, UUID> {
    List<CellEntity> findBySectorIdAndDeletedFalse(UUID sectorId);
}
