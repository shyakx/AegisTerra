package com.aegisterra.platform.application.executive;

import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.ClimateBlock;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.CountBlock;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.NotificationsBlock;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.QuickLink;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.RegionalRow;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.SettlementBlock;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.TasksBlock;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutiveOverviewService {

    private final JdbcTemplate jdbc;

    public ExecutiveOverviewService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ExecutiveOverviewResponse overview() {
        long farmersTotal = count("select count(*) from farmers where deleted = false");
        long farmersActive = count("select count(*) from farmers where deleted = false and status = 'ACTIVE'");
        long farmsTotal = count("select count(*) from farms where deleted = false");
        long farmsWithBoundary = count("""
            select count(distinct fb.farm_id) from farm_boundaries fb
            join farms f on f.id = fb.farm_id
            where f.deleted = false and coalesce(fb.deleted, false) = false
            """);
        long policiesTotal = count("select count(*) from insurance_policies where deleted = false");
        long policiesActive = count("select count(*) from insurance_policies where deleted = false and status = 'ACTIVE'");
        long policiesDraft = count("select count(*) from insurance_policies where deleted = false and status = 'DRAFT'");
        long claimsTotal = count("select count(*) from claims where deleted = false");
        long claimsOpen = count("""
            select count(*) from claims where deleted = false
            and status not in ('CLOSED','REJECTED','CANCELLED','SETTLED')
            """);
        long claimsApproved = count("select count(*) from claims where deleted = false and status in ('APPROVED','PAYMENT_PENDING','SETTLED')");
        long claimsRejected = count("select count(*) from claims where deleted = false and status = 'REJECTED'");

        long settlementsTotal = count("select count(*) from settlements where deleted = false");
        long settlementsPending = count("""
            select count(*) from settlements where deleted = false
            and status in ('PENDING','UNDER_REVIEW','APPROVED','PROCESSING','SENT','RETRY_PENDING')
            """);
        long settlementsCompleted = count("select count(*) from settlements where deleted = false and status in ('COMPLETED','CLOSED')");
        long settlementsFailed = count("select count(*) from settlements where deleted = false and status = 'FAILED'");
        Double pendingAmount = sum("""
            select coalesce(sum(amount),0) from settlements where deleted = false
            and status in ('PENDING','UNDER_REVIEW','APPROVED','PROCESSING','SENT','RETRY_PENDING')
            """);
        Double completedAmount = sum("""
            select coalesce(sum(amount),0) from settlements where deleted = false
            and status in ('COMPLETED','CLOSED')
            """);

        Timestamp recent = Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS));
        long stations = count("select count(*) from weather_stations where deleted = false");
        long recentObs = count("select count(*) from weather_observations where observed_at >= ?", recent);
        long openAlerts = count("select count(*) from climate_alerts where deleted = false and status = 'OPEN'");
        long criticalAlerts = count(
            "select count(*) from climate_alerts where deleted = false and status = 'OPEN' and severity = 'CRITICAL'"
        );

        Map<String, Long> grades = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
            """
            select coalesce(grade, 'UNKNOWN') as g, count(*) as c
            from risk_scores
            where deleted = false and farm_id is not null
            group by coalesce(grade, 'UNKNOWN')
            """
        )) {
            grades.put(String.valueOf(row.get("g")), ((Number) row.get("c")).longValue());
        }

        long pendingTasks = count(
            "select count(*) from workflow_tasks where deleted = false and status in ('PENDING','ASSIGNED','IN_PROGRESS')"
        );
        long unreadNotifications = count("select count(*) from notifications where deleted = false and read_at is null");

        List<RegionalRow> regional = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
            """
            select distinct on (district_code) district_code, score, grade, farm_count
            from district_risk_snapshots
            where deleted = false
            order by district_code, calculated_at desc
            """
        )) {
            Number score = (Number) row.get("score");
            Number farmCount = (Number) row.get("farm_count");
            regional.add(new RegionalRow(
                (String) row.get("district_code"),
                score == null ? null : score.doubleValue(),
                (String) row.get("grade"),
                farmCount == null ? 0L : farmCount.longValue()
            ));
        }

        return new ExecutiveOverviewResponse(
            Instant.now(),
            new CountBlock(farmersTotal, farmersActive, null, null, null, null, null),
            new CountBlock(farmsTotal, null, null, null, null, null, farmsWithBoundary),
            new CountBlock(policiesTotal, policiesActive, null, policiesDraft, null, null, null),
            new CountBlock(claimsTotal, null, claimsOpen, null, claimsApproved, claimsRejected, null),
            new SettlementBlock(
                settlementsTotal,
                settlementsPending,
                settlementsCompleted,
                settlementsFailed,
                pendingAmount,
                completedAmount,
                "RWF"
            ),
            new ClimateBlock(stations, recentObs, openAlerts, criticalAlerts, grades),
            new TasksBlock(pendingTasks),
            new NotificationsBlock(unreadNotifications),
            regional,
            List.of(
                new QuickLink("Farmers", "/farmers", "farmers:read"),
                new QuickLink("Policies", "/policies", "policies:read"),
                new QuickLink("Claims", "/claims", "claims:read"),
                new QuickLink("Settlements", "/settlements/dashboard", "settlements:read"),
                new QuickLink("Climate intel", "/climate-intel", "climate-intel:read"),
                new QuickLink("GIS", "/gis", "farms:read")
            )
        );
    }

    @Transactional(readOnly = true)
    public List<RegionalRow> regionalSummary() {
        return overview().regional();
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private Double sum(String sql) {
        Double value = jdbc.queryForObject(sql, Double.class);
        return value == null ? 0d : value;
    }
}
