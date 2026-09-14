package com.aegisterra.platform.infrastructure.persistence.geography;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<ProvinceEntity, UUID> {
    Optional<ProvinceEntity> findByIdAndDeletedFalse(UUID id);

    Optional<ProvinceEntity> findByCodeAndDeletedFalse(String code);

    List<ProvinceEntity> findByDeletedFalseOrderByNameAsc();
}
