package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateImportJobRepository extends JpaRepository<ClimateImportJobEntity, UUID> {
    Page<ClimateImportJobEntity> findByDeletedFalse(Pageable pageable);

    Page<ClimateImportJobEntity> findByStatusAndDeletedFalse(String status, Pageable pageable);

    long countByStatusInAndDeletedFalse(Collection<String> statuses);
}
