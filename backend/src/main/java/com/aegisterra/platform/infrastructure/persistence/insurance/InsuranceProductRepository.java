package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProductEntity, UUID> {
    Optional<InsuranceProductEntity> findByCodeAndDeletedFalse(String code);
    Optional<InsuranceProductEntity> findByIdAndDeletedFalse(UUID id);
    List<InsuranceProductEntity> findByDeletedFalseOrderByNameAsc();
}
