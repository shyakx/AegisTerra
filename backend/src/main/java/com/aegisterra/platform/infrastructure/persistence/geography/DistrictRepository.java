package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<DistrictEntity, UUID> {
    Optional<DistrictEntity> findByIdAndDeletedFalse(UUID id);

    Optional<DistrictEntity> findByCodeAndDeletedFalse(String code);

    List<DistrictEntity> findByDeletedFalseOrderByNameAsc();

    List<DistrictEntity> findByProvinceIdAndDeletedFalseOrderByNameAsc(UUID provinceId);
}
