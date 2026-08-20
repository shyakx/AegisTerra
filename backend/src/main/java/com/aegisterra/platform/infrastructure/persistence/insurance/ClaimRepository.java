package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {
    Optional<ClaimEntity> findByClaimNumberAndDeletedFalse(String claimNumber);

    Optional<ClaimEntity> findByIdAndDeletedFalse(UUID id);

    List<ClaimEntity> findByPolicyIdAndDeletedFalse(UUID policyId);

    Optional<ClaimEntity> findByWorkflowInstanceIdAndDeletedFalse(UUID workflowInstanceId);

    @Query("""
        SELECT c FROM ClaimEntity c
        WHERE c.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(c.claimNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(c.claimTypeCode, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:status IS NULL OR :status = '' OR c.status = :status)
          AND (:claimTypeCode IS NULL OR :claimTypeCode = '' OR c.claimTypeCode = :claimTypeCode)
          AND (:policyId IS NULL OR c.policyId = :policyId)
          AND (:farmerId IS NULL OR c.farmerId = :farmerId)
          AND (:fromDate IS NULL OR c.incidentDate >= :fromDate)
          AND (:toDate IS NULL OR c.incidentDate <= :toDate)
        """)
    Page<ClaimEntity> search(
        @Param("q") String q,
        @Param("status") String status,
        @Param("claimTypeCode") String claimTypeCode,
        @Param("policyId") UUID policyId,
        @Param("farmerId") UUID farmerId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable
    );

    List<ClaimEntity> findByPolicyIdAndStatusInAndDeletedFalse(UUID policyId, List<String> statuses);
}
