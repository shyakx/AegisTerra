package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<CropEntity, UUID> {
    Optional<CropEntity> findByCodeAndDeletedFalse(String code);

    List<CropEntity> findByDeletedFalseOrderByNameAsc();
}
