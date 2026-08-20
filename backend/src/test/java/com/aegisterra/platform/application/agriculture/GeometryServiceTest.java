package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.support.SharedPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GeometryServiceTest extends SharedPostgresContainer {

    @Autowired
    private GeometryService geometryService;

    @Test
    void validatesSimplePolygonAndComputesArea() {
        String geoJson = """
            {"type":"Polygon","coordinates":[[[30.0,-1.95],[30.001,-1.95],[30.001,-1.949],[30.0,-1.949],[30.0,-1.95]]]}
            """;
        var result = geometryService.validateGeoJson(geoJson);
        assertThat(result.valid()).isTrue();
        assertThat(result.areaHa()).isNotNull();
        assertThat(result.geometry()).isNotNull();
    }

    @Test
    void rejectsSelfIntersectingPolygon() {
        String geoJson = """
            {"type":"Polygon","coordinates":[[[0,0],[1,1],[1,0],[0,1],[0,0]]]}
            """;
        var result = geometryService.validateGeoJson(geoJson);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void requireValidGeometryThrowsOnInvalid() {
        String geoJson = """
            {"type":"Point","coordinates":[30.0,-1.95]}
            """;
        assertThatThrownBy(() -> geometryService.requireValidGeometry(geoJson))
            .isInstanceOf(ResponseStatusException.class);
    }
}
