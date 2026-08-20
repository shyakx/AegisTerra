package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClimateAlertRepository extends JpaRepository<ClimateAlertEntity, UUID> {
    Page<ClimateAlertEntity> findByDeletedFalse(Pageable pageable);

    Optional<ClimateAlertEntity> findByIdAndDeletedFalse(UUID id);

    long countByStatusAndDeletedFalse(String status);

    long countByStatusAndSeverityAndDeletedFalse(String status, String severity);

    List<ClimateAlertEntity> findByFarmIdAndDeletedFalseOrderByValidFromDesc(UUID farmId);

    @Query("""
        select a from ClimateAlertEntity a
        where a.deleted = false
          and (:status is null or a.status = :status)
          and (:alertType is null or a.alertType = :alertType)
          and (:severity is null or a.severity = :severity)
          and (:scopeType is null or a.scopeType = :scopeType)
        """)
    Page<ClimateAlertEntity> search(
        @Param("status") String status,
        @Param("alertType") String alertType,
        @Param("severity") String severity,
        @Param("scopeType") String scopeType,
        Pageable pageable
    );

    Optional<ClimateAlertEntity> findFirstByAlertTypeAndScopeTypeAndScopeIdAndStatusInAndDeletedFalse(
        String alertType,
        String scopeType,
        String scopeId,
        Collection<String> statuses
    );
}
