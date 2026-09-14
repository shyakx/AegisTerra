package com.aegisterra.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegisterra.platform.support.SharedPostgresContainer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class GeographyReferenceDataIT extends SharedPostgresContainer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsExpectedReferenceCounts() {
        assertThat(countActive("provinces")).isEqualTo(5);
        assertThat(countActive("districts")).isEqualTo(30);
        assertThat(countActive("agroecological_zones")).isEqualTo(5);
        assertThat(countActive("agroecological_subzones")).isEqualTo(18);
        assertThat(countActive("sectors")).isZero();
        assertThat(countActive("cells")).isZero();
        assertThat(countActive("villages")).isZero();
    }

    @Test
    void everyDistrictHasExactlyOneProvinceAndSubzone() {
        Long mapped = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM districts
            WHERE deleted = false
              AND province_id IS NOT NULL
              AND agroecological_subzone_id IS NOT NULL
            """,
            Long.class
        );
        assertThat(mapped).isEqualTo(30);

        Long unmapped = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM districts
            WHERE deleted = false
              AND (province_id IS NULL OR agroecological_subzone_id IS NULL)
            """,
            Long.class
        );
        assertThat(unmapped).isZero();
    }

    @Test
    void zoneToSubzoneMembershipMatchesWorkbook() {
        Map<String, List<String>> expected = Map.of(
            "A", List.of("A1", "A2", "A3"),
            "B", List.of("B1", "B2", "B3", "B4"),
            "C", List.of("C1", "C2", "C3", "C4"),
            "D", List.of("D1", "D2", "D3", "D4"),
            "E", List.of("E1", "E2", "E3")
        );
        expected.forEach((zone, subzones) -> {
            List<String> actual = jdbcTemplate.queryForList(
                """
                SELECT s.code FROM agroecological_subzones s
                JOIN agroecological_zones z ON z.id = s.zone_id
                WHERE z.code = ? AND z.deleted = false AND s.deleted = false
                ORDER BY s.code
                """,
                String.class,
                zone
            );
            assertThat(actual).containsExactlyElementsOf(subzones);
        });
    }

    @Test
    void codesAreUniqueAndCanonicalNamesAreClean() {
        assertThat(distinctActiveCodes("provinces")).isEqualTo(5);
        assertThat(distinctActiveCodes("districts")).isEqualTo(30);
        assertThat(distinctActiveCodes("agroecological_zones")).isEqualTo(5);
        assertThat(distinctActiveCodes("agroecological_subzones")).isEqualTo(18);

        List<String> zoneCodes = jdbcTemplate.queryForList(
            "SELECT code FROM agroecological_zones WHERE deleted = false ORDER BY code",
            String.class
        );
        assertThat(zoneCodes).containsExactly("A", "B", "C", "D", "E");

        String c1Name = jdbcTemplate.queryForObject(
            "SELECT name FROM agroecological_subzones WHERE code = 'C1' AND deleted = false",
            String.class
        );
        assertThat(c1Name).isEqualTo("Eastern Dry Savanna");
        assertThat(c1Name).doesNotContain("Sb-zone");

        String musanze = jdbcTemplate.queryForObject(
            "SELECT name FROM districts WHERE code = 'MUSANZE' AND deleted = false",
            String.class
        );
        assertThat(musanze).isEqualTo("Musanze");

        String north = jdbcTemplate.queryForObject(
            """
            SELECT p.code FROM districts d
            JOIN provinces p ON p.id = d.province_id
            WHERE d.code = 'MUSANZE' AND d.deleted = false
            """,
            String.class
        );
        assertThat(north).isEqualTo("NORTH");

        String a1 = jdbcTemplate.queryForObject(
            """
            SELECT s.code FROM districts d
            JOIN agroecological_subzones s ON s.id = d.agroecological_subzone_id
            WHERE d.code = 'MUSANZE' AND d.deleted = false
            """,
            String.class
        );
        assertThat(a1).isEqualTo("A1");
    }

    @Test
    void noFabricatedGeometryOnReferenceRows() {
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM provinces WHERE deleted = false AND geom IS NOT NULL", Long.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM districts WHERE deleted = false AND geom IS NOT NULL", Long.class
        )).isZero();
    }

    @Test
    void demoFarmsAreBackfilledWithDistrictsForPresentation() {
        Long assigned = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM farms f
            JOIN farmers r ON r.id = f.farmer_id
            WHERE r.farmer_code LIKE 'FRM-2026-%'
              AND f.deleted = false
              AND f.district_id IS NOT NULL
            """,
            Long.class
        );
        assertThat(assigned).isGreaterThanOrEqualTo(12);

        Long withAez = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM farms f
            JOIN districts d ON d.id = f.district_id AND d.deleted = false
            JOIN agroecological_subzones s ON s.id = d.agroecological_subzone_id AND s.deleted = false
            WHERE f.deleted = false
              AND f.farm_code LIKE 'FARM-2026-%'
            """,
            Long.class
        );
        assertThat(withAez).isGreaterThanOrEqualTo(12);
    }

    @Test
    void foreignKeysAndPartialUniqueIndexesExist() {
        List<String> fks = jdbcTemplate.queryForList(
            """
            SELECT constraint_name FROM information_schema.table_constraints
            WHERE table_schema = 'public'
              AND constraint_type = 'FOREIGN KEY'
              AND constraint_name IN (
                'fk_districts_province',
                'fk_districts_agroecological_subzone',
                'fk_agroecological_subzones_zone',
                'fk_farms_district'
              )
            ORDER BY constraint_name
            """,
            String.class
        );
        assertThat(fks).containsExactly(
            "fk_agroecological_subzones_zone",
            "fk_districts_agroecological_subzone",
            "fk_districts_province",
            "fk_farms_district"
        );

        List<String> indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname IN (
                'uk_provinces_code_active',
                'uk_districts_code_active',
                'uk_agroecological_zones_code_active',
                'uk_agroecological_subzones_code_active'
              )
            ORDER BY indexname
            """,
            String.class
        );
        assertThat(indexes).containsExactly(
            "uk_agroecological_subzones_code_active",
            "uk_agroecological_zones_code_active",
            "uk_districts_code_active",
            "uk_provinces_code_active"
        );
    }

    private long countActive(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE deleted = false", Long.class);
        return count == null ? 0 : count;
    }

    private long distinctActiveCodes(String table) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT count(DISTINCT code) FROM " + table + " WHERE deleted = false",
            Long.class
        );
        return count == null ? 0 : count;
    }
}
