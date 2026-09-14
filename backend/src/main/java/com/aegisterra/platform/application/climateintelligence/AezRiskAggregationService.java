package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.application.contracts.AezRiskSummaryResponse;
import com.aegisterra.platform.application.contracts.AezRiskSummaryResponse.SubzoneHeat;
import com.aegisterra.platform.application.contracts.AezRiskSummaryResponse.ZoneHeat;
import com.aegisterra.platform.application.contracts.NationalRiskDashboardResponse;
import com.aegisterra.platform.domain.climateintelligence.RiskGrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-time AEZ Zone/Sub-zone climate risk aggregation.
 * Uses latest-per-district snapshots (same selection rule as ExecutiveOverviewService),
 * joins {@code districts.code}, and excludes unmapped climate codes such as {@code KIGALI}.
 * Does not alter RiskEngine formulas or rewrite historical rows.
 */
@Service
public class AezRiskAggregationService {

    private final JdbcTemplate jdbc;

    public AezRiskAggregationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Latest district risk snapshot per {@code district_code}, ordered by score descending.
     * Avoids historical duplicates that {@code findByDeletedFalseOrderByScoreDesc} would surface.
     */
    @Transactional(readOnly = true)
    public List<NationalRiskDashboardResponse.DistrictHeat> latestDistrictHeat() {
        List<NationalRiskDashboardResponse.DistrictHeat> heat = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
            """
            SELECT DISTINCT ON (district_code)
                   district_code, score, grade
            FROM district_risk_snapshots
            WHERE deleted = false
            ORDER BY district_code, calculated_at DESC
            """
        )) {
            Double score = toDouble(row.get("score"));
            heat.add(new NationalRiskDashboardResponse.DistrictHeat(
                (String) row.get("district_code"),
                score == null ? null : round(score),
                (String) row.get("grade")
            ));
        }
        heat.sort(Comparator
            .comparing((NationalRiskDashboardResponse.DistrictHeat h) -> h.meanScore() == null ? Double.NEGATIVE_INFINITY : h.meanScore())
            .reversed());
        return heat;
    }

    @Transactional(readOnly = true)
    public AezRiskSummaryResponse summarize() {
        List<ZoneHeat> zones = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
            """
            WITH latest AS (
              SELECT DISTINCT ON (district_code)
                     district_code, score, farm_count
              FROM district_risk_snapshots
              WHERE deleted = false
              ORDER BY district_code, calculated_at DESC
            ),
            mapped AS (
              SELECT l.score, l.farm_count,
                     z.code AS zone_code, z.name AS zone_name
              FROM latest l
              JOIN districts d ON d.code = l.district_code AND d.deleted = false
              JOIN agroecological_subzones s
                ON s.id = d.agroecological_subzone_id AND s.deleted = false
              JOIN agroecological_zones z
                ON z.id = s.zone_id AND z.deleted = false
            )
            SELECT zone_code, zone_name,
                   avg(score::float8) AS mean_score,
                   count(*)::int AS district_count,
                   coalesce(sum(farm_count), 0)::int AS farm_count
            FROM mapped
            GROUP BY zone_code, zone_name
            ORDER BY zone_code
            """
        )) {
            Double mean = toDouble(row.get("mean_score"));
            zones.add(new ZoneHeat(
                (String) row.get("zone_code"),
                (String) row.get("zone_name"),
                mean == null ? null : round(mean),
                mean == null ? RiskGrade.INSUFFICIENT_DATA.name() : RiskGrade.fromScore(mean).name(),
                ((Number) row.get("district_count")).intValue(),
                ((Number) row.get("farm_count")).intValue()
            ));
        }

        List<SubzoneHeat> subzones = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
            """
            WITH latest AS (
              SELECT DISTINCT ON (district_code)
                     district_code, score, farm_count
              FROM district_risk_snapshots
              WHERE deleted = false
              ORDER BY district_code, calculated_at DESC
            ),
            mapped AS (
              SELECT l.score, l.farm_count,
                     s.code AS subzone_code, s.name AS subzone_name,
                     z.code AS zone_code, z.name AS zone_name
              FROM latest l
              JOIN districts d ON d.code = l.district_code AND d.deleted = false
              JOIN agroecological_subzones s
                ON s.id = d.agroecological_subzone_id AND s.deleted = false
              JOIN agroecological_zones z
                ON z.id = s.zone_id AND z.deleted = false
            )
            SELECT subzone_code, subzone_name, zone_code, zone_name,
                   avg(score::float8) AS mean_score,
                   count(*)::int AS district_count,
                   coalesce(sum(farm_count), 0)::int AS farm_count
            FROM mapped
            GROUP BY subzone_code, subzone_name, zone_code, zone_name
            ORDER BY zone_code, subzone_code
            """
        )) {
            Double mean = toDouble(row.get("mean_score"));
            subzones.add(new SubzoneHeat(
                (String) row.get("subzone_code"),
                (String) row.get("subzone_name"),
                (String) row.get("zone_code"),
                (String) row.get("zone_name"),
                mean == null ? null : round(mean),
                mean == null ? RiskGrade.INSUFFICIENT_DATA.name() : RiskGrade.fromScore(mean).name(),
                ((Number) row.get("district_count")).intValue(),
                ((Number) row.get("farm_count")).intValue()
            ));
        }

        List<String> unmapped = jdbc.queryForList(
            """
            WITH latest AS (
              SELECT DISTINCT ON (district_code)
                     district_code
              FROM district_risk_snapshots
              WHERE deleted = false
              ORDER BY district_code, calculated_at DESC
            )
            SELECT l.district_code
            FROM latest l
            LEFT JOIN districts d ON d.code = l.district_code AND d.deleted = false
            WHERE d.id IS NULL
               OR d.agroecological_subzone_id IS NULL
            ORDER BY l.district_code
            """,
            String.class
        );

        return new AezRiskSummaryResponse(zones, subzones, unmapped, unmapped.size(), Instant.now());
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
