package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgroecologicalZoneRepository extends JpaRepository<AgroecologicalZoneEntity, UUID> {
    Optional<AgroecologicalZoneEntity> findByIdAndDeletedFalse(UUID id);

    Optional<AgroecologicalZoneEntity> findByCodeAndDeletedFalse(String code);

    List<AgroecologicalZoneEntity> findByDeletedFalseOrderByCodeAsc();
}
