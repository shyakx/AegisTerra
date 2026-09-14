package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.application.contracts.NationalRiskDashboardResponse;
import com.aegisterra.platform.application.contracts.YieldClimateOutlookResponse;
import com.aegisterra.platform.application.contracts.YieldClimateOutlookResponse.CropOutlook;
import com.aegisterra.platform.application.contracts.YieldClimateOutlookResponse.YearYield;
import com.aegisterra.platform.domain.climateintelligence.RiskGrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-time yield + climate planning outlook.
 * Uses recorded {@code crop_seasons.yield_t_ha} and latest district risk (no ML).
 */
@Service
public class YieldClimateOutlookService {

    private static final String METHODOLOGY =
        "Past = mean yield in seasons ending years Y-10..Y-3; "
            + "Recent = mean yield in Y-2..Y-1; "
            + "Predicted = recent + 0.5×(recent−past) × (1 − 0.35×nationalRisk/100). "
            + "Explainable rule only — not a machine-learning forecast.";

    private final JdbcTemplate jdbc;
    private final AezRiskAggregationService aezRiskAggregationService;

    public YieldClimateOutlookService(JdbcTemplate jdbc, AezRiskAggregationService aezRiskAggregationService) {
        this.jdbc = jdbc;
        this.aezRiskAggregationService = aezRiskAggregationService;
    }

    @Transactional(readOnly = true)
    public YieldClimateOutlookResponse outlook() {
        int y = LocalDate.now().getYear();
        int pastStart = y - 10;
        int pastEnd = y - 3;
        int recentStart = y - 2;
        int recentEnd = y - 1;

        List<NationalRiskDashboardResponse.DistrictHeat> heat = aezRiskAggregationService.latestDistrictHeat();
        Double nationalRisk = meanScore(heat);
        String nationalGrade = nationalRisk == null
            ? RiskGrade.INSUFFICIENT_DATA.name()
            : RiskGrade.fromScore(nationalRisk).name();

        List<Map<String, Object>> rows = jdbc.queryForList(
            """
            SELECT c.code AS crop_code,
                   c.name AS crop_name,
                   EXTRACT(YEAR FROM s.end_date)::int AS harvest_year,
                   avg(cs.yield_t_ha)::float8 AS mean_yield,
                   count(*)::int AS sample_count,
                   count(DISTINCT cs.farm_id)::int AS farm_count
            FROM crop_seasons cs
            JOIN crops c ON c.id = cs.crop_id AND c.deleted = false
            JOIN seasons s ON s.id = cs.season_id AND s.deleted = false
            WHERE cs.deleted = false
              AND cs.yield_t_ha IS NOT NULL
              AND EXTRACT(YEAR FROM s.end_date)::int BETWEEN ? AND ?
            GROUP BY c.code, c.name, EXTRACT(YEAR FROM s.end_date)::int
            ORDER BY c.code, harvest_year
            """,
            pastStart,
            y
        );

        Map<String, List<Map<String, Object>>> byCrop = rows.stream()
            .collect(Collectors.groupingBy(
                r -> (String) r.get("crop_code"),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<CropOutlook> crops = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byCrop.entrySet()) {
            List<Map<String, Object>> seriesRows = entry.getValue();
            String cropCode = entry.getKey();
            String cropName = (String) seriesRows.getFirst().get("crop_name");

            List<YearYield> series = new ArrayList<>();
            List<Double> pastYields = new ArrayList<>();
            List<Double> recentYields = new ArrayList<>();
            int farmCount = 0;
            int sampleCount = 0;

            for (Map<String, Object> row : seriesRows) {
                int year = ((Number) row.get("harvest_year")).intValue();
                Double mean = toDouble(row.get("mean_yield"));
                int samples = ((Number) row.get("sample_count")).intValue();
                farmCount = Math.max(farmCount, ((Number) row.get("farm_count")).intValue());
                sampleCount += samples;
                series.add(new YearYield(year, mean == null ? null : round(mean), samples));
                if (mean == null) {
                    continue;
                }
                if (year >= pastStart && year <= pastEnd) {
                    pastYields.add(mean);
                }
                if (year >= recentStart && year <= recentEnd) {
                    recentYields.add(mean);
                }
            }

            Double pastMean = average(pastYields);
            Double recentMean = average(recentYields);
            if (recentMean == null && !series.isEmpty()) {
                // Fallback: newest available year if recent window empty
                YearYield last = series.getLast();
                recentMean = last.meanYieldTHa();
            }
            Double predicted = predict(pastMean, recentMean, nationalRisk);
            Double changePct = null;
            if (pastMean != null && pastMean > 0 && recentMean != null) {
                changePct = round(((recentMean - pastMean) / pastMean) * 100.0);
            }
            String label = outlookLabel(pastMean, recentMean, predicted, nationalRisk);
            String narrative = narrative(cropName, pastMean, recentMean, predicted, nationalRisk, nationalGrade);

            crops.add(new CropOutlook(
                cropCode,
                cropName,
                pastMean == null ? null : round(pastMean),
                recentMean == null ? null : round(recentMean),
                predicted == null ? null : round(predicted),
                changePct,
                label,
                narrative,
                farmCount,
                sampleCount,
                series
            ));
        }

        crops.sort(Comparator.comparing(CropOutlook::cropCode));

        return new YieldClimateOutlookResponse(
            y,
            pastStart,
            pastEnd,
            recentStart,
            recentEnd,
            nationalRisk == null ? null : round(nationalRisk),
            nationalGrade,
            crops,
            METHODOLOGY,
            Instant.now()
        );
    }

    private static Double predict(Double pastMean, Double recentMean, Double nationalRisk) {
        if (recentMean == null) {
            return null;
        }
        double trend = pastMean == null ? 0.0 : (recentMean - pastMean);
        double stress = nationalRisk == null ? 0.35 : Math.min(1.0, Math.max(0.0, nationalRisk / 100.0));
        double dampen = 1.0 - (0.35 * stress);
        return Math.max(0.0, (recentMean + 0.5 * trend) * dampen);
    }

    private static String outlookLabel(Double past, Double recent, Double predicted, Double risk) {
        if (predicted == null || recent == null) {
            return "INSUFFICIENT_DATA";
        }
        double drop = recent - predicted;
        if (risk != null && risk >= 70 && drop > 0) {
            return "ELEVATED_LOSS_RISK";
        }
        if (predicted < recent * 0.92) {
            return "YIELD_PRESSURE";
        }
        if (past != null && recent < past * 0.95) {
            return "DECLINING_TREND";
        }
        return "STABLE_WATCH";
    }

    private static String narrative(
        String cropName,
        Double past,
        Double recent,
        Double predicted,
        Double risk,
        String grade
    ) {
        String pastTxt = past == null ? "limited" : String.format("%.2f t/ha", past);
        String recentTxt = recent == null ? "limited" : String.format("%.2f t/ha", recent);
        String predTxt = predicted == null ? "not yet estimable" : String.format("%.2f t/ha", predicted);
        String riskTxt = risk == null ? "insufficient climate risk data" : String.format("%.1f (%s)", risk, grade);
        return String.format(
            "For %s, past window averaged %s; recent seasons averaged %s. "
                + "Under current national climate risk %s, the explainable outlook for the next season is %s.",
            cropName,
            pastTxt,
            recentTxt,
            riskTxt,
            predTxt
        );
    }

    @Transactional(readOnly = true)
    public String outlookCsv() {
        YieldClimateOutlookResponse o = outlook();
        StringBuilder sb = new StringBuilder();
        sb.append("crop_code,crop_name,past_mean_yield_t_ha,recent_mean_yield_t_ha,predicted_yield_t_ha,")
            .append("yield_change_pct_recent_vs_past,outlook_label,farm_count,season_sample_count,")
            .append("national_mean_risk_score,national_risk_grade,reference_year,generated_at\n");
        for (CropOutlook c : o.crops()) {
            sb.append(csv(c.cropCode())).append(',')
                .append(csv(c.cropName())).append(',')
                .append(num(c.pastMeanYieldTHa())).append(',')
                .append(num(c.recentMeanYieldTHa())).append(',')
                .append(num(c.predictedYieldTHa())).append(',')
                .append(num(c.yieldChangePctRecentVsPast())).append(',')
                .append(csv(c.outlookLabel())).append(',')
                .append(c.farmCount()).append(',')
                .append(c.seasonSampleCount()).append(',')
                .append(num(o.nationalMeanRiskScore())).append(',')
                .append(csv(o.nationalRiskGrade())).append(',')
                .append(o.referenceYear()).append(',')
                .append(csv(o.generatedAt().toString()))
                .append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String num(Double value) {
        return value == null ? "" : Double.toString(value);
    }

    private static Double meanScore(List<NationalRiskDashboardResponse.DistrictHeat> heat) {
        List<Double> scores = heat.stream()
            .map(NationalRiskDashboardResponse.DistrictHeat::meanScore)
            .filter(s -> s != null)
            .toList();
        return average(scores);
    }

    private static Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        double sum = 0;
        for (Double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
