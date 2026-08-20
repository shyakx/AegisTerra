package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdRepository extends JpaRepository<HouseholdEntity, UUID> {
    Optional<HouseholdEntity> findByCodeAndDeletedFalse(String code);

    Optional<HouseholdEntity> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT h FROM HouseholdEntity h
        WHERE h.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(h.code) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(h.headName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:status IS NULL OR :status = '' OR h.status = :status)
        """)
    Page<HouseholdEntity> search(@Param("q") String q, @Param("status") String status, Pageable pageable);

    List<HouseholdEntity> findByDeletedFalseOrderByCodeAsc();
}
