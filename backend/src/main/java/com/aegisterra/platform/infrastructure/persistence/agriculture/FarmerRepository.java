package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FarmerRepository extends JpaRepository<FarmerEntity, UUID> {
    Optional<FarmerEntity> findByNationalIdAndDeletedFalse(String nationalId);

    Optional<FarmerEntity> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    Optional<FarmerEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<FarmerEntity> findByFarmerCodeAndDeletedFalse(String farmerCode);

    Optional<FarmerEntity> findByIdAndDeletedFalse(UUID id);

    Optional<FarmerEntity> findByUserIdAndDeletedFalse(UUID userId);

    List<FarmerEntity> findByDeletedFalseOrderByLastNameAsc();

    @Query("""
        SELECT f FROM FarmerEntity f
        WHERE f.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(f.firstName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(f.lastName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(f.nationalId) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(f.phoneNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(f.farmerCode, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(f.email, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:nationalId IS NULL OR :nationalId = '' OR f.nationalId = :nationalId)
          AND (:phone IS NULL OR :phone = '' OR f.phoneNumber = :phone)
          AND (:farmerCode IS NULL OR :farmerCode = '' OR f.farmerCode = :farmerCode)
          AND (:householdId IS NULL OR f.householdId = :householdId)
          AND (:districtId IS NULL OR f.districtId = :districtId)
          AND (:villageId IS NULL OR f.villageId = :villageId)
          AND (:status IS NULL OR :status = '' OR f.status = :status)
        """)
    Page<FarmerEntity> search(
        @Param("q") String q,
        @Param("nationalId") String nationalId,
        @Param("phone") String phone,
        @Param("farmerCode") String farmerCode,
        @Param("householdId") UUID householdId,
        @Param("districtId") UUID districtId,
        @Param("villageId") UUID villageId,
        @Param("status") String status,
        Pageable pageable
    );
}
