package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicyEntity, UUID> {
    Optional<InsurancePolicyEntity> findByPolicyNumberAndDeletedFalse(String policyNumber);

    Optional<InsurancePolicyEntity> findByIdAndDeletedFalse(UUID id);

    List<InsurancePolicyEntity> findByFarmerIdAndDeletedFalse(UUID farmerId);

    List<InsurancePolicyEntity> findByFarmIdAndDeletedFalse(UUID farmId);

    @Query("""
        SELECT p FROM InsurancePolicyEntity p
        WHERE p.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:policyNumber IS NULL OR :policyNumber = '' OR p.policyNumber = :policyNumber)
          AND (:farmerId IS NULL OR p.farmerId = :farmerId)
          AND (:farmId IS NULL OR p.farmId = :farmId)
          AND (:productId IS NULL OR p.productId = :productId)
          AND (:seasonId IS NULL OR p.seasonId = :seasonId)
          AND (:cropId IS NULL OR p.cropId = :cropId)
          AND (:companyId IS NULL OR p.insuranceCompanyId = :companyId)
          AND (:status IS NULL OR :status = '' OR p.status = :status)
        """)
    Page<InsurancePolicyEntity> search(
        @Param("q") String q,
        @Param("policyNumber") String policyNumber,
        @Param("farmerId") UUID farmerId,
        @Param("farmId") UUID farmId,
        @Param("productId") UUID productId,
        @Param("seasonId") UUID seasonId,
        @Param("cropId") UUID cropId,
        @Param("companyId") UUID companyId,
        @Param("status") String status,
        Pageable pageable
    );

    long countByDeletedFalseAndStatus(String status);
}
