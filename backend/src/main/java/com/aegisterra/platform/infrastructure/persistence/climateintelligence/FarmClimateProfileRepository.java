package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmClimateProfileRepository extends JpaRepository<FarmClimateProfileEntity, UUID> {
    Optional<FarmClimateProfileEntity> findFirstByFarmIdAndDeletedFalseOrderByGeneratedAtDesc(UUID farmId);

    List<FarmClimateProfileEntity> findByFarmIdAndDeletedFalseOrderByGeneratedAtDesc(UUID farmId);
}
