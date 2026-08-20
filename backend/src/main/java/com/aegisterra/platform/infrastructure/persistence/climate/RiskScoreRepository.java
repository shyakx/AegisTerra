package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskScoreRepository extends JpaRepository<RiskScoreEntity, UUID> {
    Optional<RiskScoreEntity> findFirstByFarmIdAndDeletedFalseOrderByCalculatedAtDesc(UUID farmId);

    List<RiskScoreEntity> findByDeletedFalseAndFarmIdIsNotNull();

    List<RiskScoreEntity> findByDistrictCodeAndDeletedFalse(String districtCode);
}
