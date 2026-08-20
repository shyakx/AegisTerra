package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateValidationResultRepository extends JpaRepository<ClimateValidationResultEntity, UUID> {
    List<ClimateValidationResultEntity> findByImportJobIdOrderByOccurredAtAsc(UUID importJobId);
}
