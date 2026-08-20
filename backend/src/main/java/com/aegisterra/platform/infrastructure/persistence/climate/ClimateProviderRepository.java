package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateProviderRepository extends JpaRepository<ClimateProviderEntity, UUID> {
    List<ClimateProviderEntity> findByDeletedFalseOrderByCodeAsc();

    Optional<ClimateProviderEntity> findByCodeAndDeletedFalse(String code);

    long countByDeletedFalse();

    long countByEnabledTrueAndDeletedFalse();
}
