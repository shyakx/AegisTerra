package com.aegisterra.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.support.SharedPostgresContainer;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FarmBoundaryRepositoryIT extends SharedPostgresContainer {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmBoundaryRepository farmBoundaryRepository;

    @Test
    void savesValidPolygonAndComputesArea() {
        FarmerEntity farmer = farmerRepository.save(
            FarmerEntity.create("Jean", "Uwimana", "NID-FB-001", "+250788000001")
        );
        FarmEntity farm = farmRepository.save(FarmEntity.create(farmer.getId(), "FARM-FB-001", "Demo Farm"));

        MultiPolygon geom = smallMultiPolygonNearKigali();
        FarmBoundaryEntity boundary = farmBoundaryRepository.save(FarmBoundaryEntity.create(farm.getId(), geom));

        assertThat(farmBoundaryRepository.isGeometryValid(boundary.getId())).isTrue();
        Double areaHa = farmBoundaryRepository.calculateAreaHectares(boundary.getId());
        assertThat(areaHa).isNotNull().isGreaterThan(0.0);
    }

    private static MultiPolygon smallMultiPolygonNearKigali() {
        Coordinate[] ring = new Coordinate[] {
            new Coordinate(30.05, -1.95),
            new Coordinate(30.0505, -1.95),
            new Coordinate(30.0505, -1.9505),
            new Coordinate(30.05, -1.9505),
            new Coordinate(30.05, -1.95)
        };
        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(ring);
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell);
        return GEOMETRY_FACTORY.createMultiPolygon(new Polygon[] { polygon });
    }
}
