package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FarmBoundaryRepository extends JpaRepository<FarmBoundaryEntity, UUID> {
    List<FarmBoundaryEntity> findByFarmIdAndDeletedFalse(UUID farmId);

    @Query(value = "SELECT ST_IsValid(geom) FROM farm_boundaries WHERE id = :id", nativeQuery = true)
    Boolean isGeometryValid(@Param("id") UUID id);

    @Query(value = "SELECT ST_Area(geom::geography) / 10000.0 FROM farm_boundaries WHERE id = :id", nativeQuery = true)
    Double calculateAreaHectares(@Param("id") UUID id);
}
