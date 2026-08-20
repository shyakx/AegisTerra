package com.aegisterra.platform.infrastructure.persistence.climate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SatelliteObservationRepository extends JpaRepository<SatelliteObservationEntity, UUID> {
    Page<SatelliteObservationEntity> findByObservedAtBetween(Instant from, Instant to, Pageable pageable);

    Page<SatelliteObservationEntity> findByFarmIdAndObservedAtBetween(
        UUID farmId,
        Instant from,
        Instant to,
        Pageable pageable
    );

    List<SatelliteObservationEntity> findByFootprintIsNotNull();
}
