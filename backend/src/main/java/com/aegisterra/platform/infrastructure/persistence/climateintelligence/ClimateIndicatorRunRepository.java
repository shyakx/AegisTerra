package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateIndicatorRunRepository extends JpaRepository<ClimateIndicatorRunEntity, UUID> {
    Page<ClimateIndicatorRunEntity> findByIndicatorType(String indicatorType, Pageable pageable);

    Page<ClimateIndicatorRunEntity> findBySubjectTypeAndSubjectId(
        String subjectType,
        String subjectId,
        Pageable pageable
    );

    List<ClimateIndicatorRunEntity> findBySubjectTypeAndSubjectIdOrderByCalculatedAtDesc(
        String subjectType,
        String subjectId
    );

    Optional<ClimateIndicatorRunEntity> findFirstByIndicatorTypeAndSubjectTypeAndSubjectIdOrderByCalculatedAtDesc(
        String indicatorType,
        String subjectType,
        String subjectId
    );
}
