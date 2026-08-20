package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FarmRepository extends JpaRepository<FarmEntity, UUID> {
    List<FarmEntity> findByFarmerIdAndDeletedFalse(UUID farmerId);

    Optional<FarmEntity> findByFarmCodeAndDeletedFalse(String farmCode);

    Optional<FarmEntity> findByIdAndDeletedFalse(UUID id);

    boolean existsByFarmerIdAndFarmNameIgnoreCaseAndDeletedFalse(UUID farmerId, String farmName);

    boolean existsByFarmerIdAndFarmNameIgnoreCaseAndDeletedFalseAndIdNot(UUID farmerId, String farmName, UUID id);

    @Query("""
        SELECT f FROM FarmEntity f
        WHERE f.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(f.farmName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(f.farmCode) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:farmerId IS NULL OR f.farmerId = :farmerId)
          AND (:districtId IS NULL OR f.districtId = :districtId)
          AND (:villageId IS NULL OR f.villageId = :villageId)
          AND (:status IS NULL OR :status = '' OR f.status = :status)
        """)
    Page<FarmEntity> search(
        @Param("q") String q,
        @Param("farmerId") UUID farmerId,
        @Param("districtId") UUID districtId,
        @Param("villageId") UUID villageId,
        @Param("status") String status,
        Pageable pageable
    );
}
