package com.aegisterra.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegisterra.platform.support.SharedPostgresContainer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class LiquibaseMigrationIT extends SharedPostgresContainer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithPostgisAndCoreTables() {
        String postgisVersion = jdbcTemplate.queryForObject("SELECT PostGIS_Version()", String.class);
        assertThat(postgisVersion).isNotBlank();

        List<String> tables = jdbcTemplate.queryForList(
            """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name IN (
                'farmers', 'farms', 'farm_boundaries', 'districts',
                'insurance_policies', 'claims', 'payouts',
                'weather_stations', 'configurations',
                'climate_providers', 'climate_import_jobs', 'climate_alerts',
                'climate_rule_sets', 'farm_climate_profiles'
              )
            ORDER BY table_name
            """,
            String.class
        );

        assertThat(tables).containsExactly(
            "claims",
            "climate_alerts",
            "climate_import_jobs",
            "climate_providers",
            "climate_rule_sets",
            "configurations",
            "districts",
            "farm_boundaries",
            "farm_climate_profiles",
            "farmers",
            "farms",
            "insurance_policies",
            "payouts",
            "weather_stations"
        );

        Long demoFarmers = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM farmers WHERE farmer_code LIKE 'FRM-2026-%' AND deleted = false",
            Long.class
        );
        assertThat(demoFarmers).isGreaterThanOrEqualTo(8);
    }

    @Test
    void demoDataIncludesOperationalWorkflowAndClaimDetails() {
        Long workflowTasks = jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM workflow_tasks wt
            WHERE wt.subject_type = 'CLAIM'
              AND wt.deleted = false
              AND wt.subject_id IN (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee701'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee702'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee703'::uuid
              )
            """,
            Long.class
        );
        assertThat(workflowTasks).isGreaterThanOrEqualTo(3);

        String claimTaskRole = jdbcTemplate.queryForObject(
            """
            SELECT assignee_role_code
            FROM workflow_tasks
            WHERE id = 'dddddddd-dddd-dddd-dddd-ddddddddd023'::uuid
              AND deleted = false
            """,
            String.class
        );
        assertThat(claimTaskRole).isEqualTo("INSURANCE_OFFICER");

        String settlementTaskRole = jdbcTemplate.queryForObject(
            """
            SELECT assignee_role_code
            FROM workflow_tasks
            WHERE id = 'dddddddd-dddd-dddd-dddd-ddddddddd024'::uuid
              AND deleted = false
            """,
            String.class
        );
        assertThat(settlementTaskRole).isEqualTo("FI_OFFICER");

        String adminOpenTaskRole = jdbcTemplate.queryForObject(
            """
            SELECT assignee_role_code
            FROM workflow_tasks
            WHERE id = 'dddddddd-dddd-dddd-dddd-ddddddddd029'::uuid
              AND deleted = false
              AND status = 'PENDING'
            """,
            String.class
        );
        assertThat(adminOpenTaskRole).isEqualTo("SYSTEM_ADMIN");

        Long inspections = jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM claim_inspections ci
            WHERE ci.claim_id IN (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee701'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee702'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee703'::uuid
            )
              AND ci.deleted = false
            """,
            Long.class
        );
        assertThat(inspections).isGreaterThanOrEqualTo(2);

        Long assessments = jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM claim_assessments ca
            WHERE ca.claim_id IN (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee701'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee702'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeee703'::uuid
            )
              AND ca.deleted = false
            """,
            Long.class
        );
        assertThat(assessments).isGreaterThanOrEqualTo(2);

        Long linkedHeroClaims = jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM claims c
            WHERE c.claim_number IN ('CLM-2026-001', 'CLM-2026-003')
              AND c.deleted = false
              AND c.workflow_instance_id IS NOT NULL
              AND c.workflow_definition_code = 'CLAIM_STANDARD'
            """,
            Long.class
        );
        assertThat(linkedHeroClaims).isEqualTo(2);

        Long heroWorkflowEvents = jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM workflow_events e
            WHERE e.deleted = false
              AND e.instance_id IN (
                'cccccccc-cccc-cccc-cccc-ccccccccc101'::uuid,
                'cccccccc-cccc-cccc-cccc-ccccccccc103'::uuid
              )
            """,
            Long.class
        );
        assertThat(heroWorkflowEvents).isGreaterThanOrEqualTo(10);
    }
}