package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgroecologicalSubzoneRepository extends JpaRepository<AgroecologicalSubzoneEntity, UUID> {
    Optional<AgroecologicalSubzoneEntity> findByIdAndDeletedFalse(UUID id);

    Optional<AgroecologicalSubzoneEntity> findByCodeAndDeletedFalse(String code);

    List<AgroecologicalSubzoneEntity> findByDeletedFalseOrderByCodeAsc();

    List<AgroecologicalSubzoneEntity> findByZoneIdAndDeletedFalseOrderByCodeAsc(UUID zoneId);
}
