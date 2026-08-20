package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRiskSnapshotRepository extends JpaRepository<DistrictRiskSnapshotEntity, UUID> {
    Optional<DistrictRiskSnapshotEntity> findFirstByDistrictCodeAndDeletedFalseOrderByCalculatedAtDesc(
        String districtCode
    );

    List<DistrictRiskSnapshotEntity> findByDeletedFalseOrderByScoreDesc();
}
