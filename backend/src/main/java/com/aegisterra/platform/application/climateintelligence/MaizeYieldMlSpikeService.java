package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.application.contracts.MaizeYieldMlSpikeResponse;
import com.aegisterra.platform.application.contracts.MaizeYieldMlSpikeResponse.FeatureRow;
import com.aegisterra.platform.application.contracts.MaizeYieldMlSpikeResponse.FoldResult;
import com.aegisterra.platform.application.contracts.MaizeYieldMlSpikeResponse.MetricsSummary;
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
 * Stage A ML spike: extract maize yearly features and compare simple predictors
 * with leave-one-year-out MAE. No production model registry.
 */
@Service
public class MaizeYieldMlSpikeService {

    private static final String CROP = "MAIZE";

    private final JdbcTemplate jdbc;

    public MaizeYieldMlSpikeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public MaizeYieldMlSpikeResponse evaluate() {
        List<FeatureRow> features = buildFeatures();
        List<FoldResult> folds = new ArrayList<>();

        for (FeatureRow holdout : features) {
            List<FeatureRow> train = features.stream()
                .filter(r -> r.harvestYear() != holdout.harvestYear())
                .toList();
            if (train.size() < 3) {
                continue;
            }

            Double naive = lastYearYield(features, holdout.harvestYear());
            Double rule = recentMeanOfPrior(features, holdout.harvestYear(), 2);
            Double linear = fitPredictLinear(train, holdout);

            folds.add(new FoldResult(
                holdout.harvestYear(),
                holdout.meanYieldTHa(),
                naive == null ? null : round(naive),
                rule == null ? null : round(rule),
                linear == null ? null : round(linear),
                absErr(holdout.meanYieldTHa(), naive),
                absErr(holdout.meanYieldTHa(), rule),
                absErr(holdout.meanYieldTHa(), linear)
            ));
        }

        Double maeNaive = meanAbs(folds.stream().map(FoldResult::absErrorNaive).toList());
        Double maeRule = meanAbs(folds.stream().map(FoldResult::absErrorRule).toList());
        Double maeLinear = meanAbs(folds.stream().map(FoldResult::absErrorLinear).toList());
        String best = bestMethod(maeNaive, maeRule, maeLinear);
        boolean linearBeats = maeLinear != null && maeRule != null && maeLinear < maeRule;

        String verdict;
        if (folds.isEmpty()) {
            verdict = "INSUFFICIENT_DATA";
        } else if (linearBeats) {
            verdict = "LINEAR_BEATS_RULE — candidate for Stage B batch inference after real yield/climate volume grows";
        } else {
            verdict = "RULE_HOLDS — keep explainable rule as default; continue collecting features";
        }

        String notes = "Features: harvest year maize mean yield, lag-1/lag-2 yield, demo SEASON rain (ml_spike). "
            + "Linear model: yield ~ lag1 + season_rain (OLS, leave-one-year-out). "
            + "Not production ML; Climate 8B remains rule-first (ADR-010).";

        return new MaizeYieldMlSpikeResponse(
            CROP,
            features.size(),
            features,
            folds,
            new MetricsSummary(
                folds.size(),
                maeNaive == null ? null : round(maeNaive),
                maeRule == null ? null : round(maeRule),
                maeLinear == null ? null : round(maeLinear),
                best,
                linearBeats
            ),
            verdict,
            notes,
            Instant.now()
        );
    }

    private List<FeatureRow> buildFeatures() {
        List<Map<String, Object>> yieldRows = jdbc.queryForList(
            """
            SELECT EXTRACT(YEAR FROM s.end_date)::int AS harvest_year,
                   avg(cs.yield_t_ha)::float8 AS mean_yield,
                   count(*)::int AS sample_count,
                   count(DISTINCT cs.farm_id)::int AS farm_count
            FROM crop_seasons cs
            JOIN crops c ON c.id = cs.crop_id AND c.deleted = false
            JOIN seasons s ON s.id = cs.season_id AND s.deleted = false
            WHERE cs.deleted = false
              AND cs.yield_t_ha IS NOT NULL
              AND c.code = ?
            GROUP BY EXTRACT(YEAR FROM s.end_date)::int
            ORDER BY harvest_year
            """,
            CROP
        );

        Map<Integer, Double> rainByYear = jdbc.query(
            """
            SELECT EXTRACT(YEAR FROM period_end AT TIME ZONE 'UTC')::int AS y,
                   avg(sum_value)::float8 AS rain_mm
            FROM climate_observation_aggregates
            WHERE variable_code = 'RAIN_MM'
              AND period_type = 'SEASON'
              AND source = 'ML_SPIKE_DEMO'
            GROUP BY EXTRACT(YEAR FROM period_end AT TIME ZONE 'UTC')::int
            """,
            (rs, rowNum) -> Map.entry(rs.getInt("y"), rs.getDouble("rain_mm"))
        ).stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        List<FeatureRow> rows = new ArrayList<>();
        for (int i = 0; i < yieldRows.size(); i++) {
            Map<String, Object> row = yieldRows.get(i);
            int year = ((Number) row.get("harvest_year")).intValue();
            double mean = ((Number) row.get("mean_yield")).doubleValue();
            Double lag1 = i > 0 ? ((Number) yieldRows.get(i - 1).get("mean_yield")).doubleValue() : null;
            Double lag2 = i > 1 ? ((Number) yieldRows.get(i - 2).get("mean_yield")).doubleValue() : null;
            rows.add(new FeatureRow(
                year,
                round(mean),
                ((Number) row.get("sample_count")).intValue(),
                ((Number) row.get("farm_count")).intValue(),
                lag1 == null ? null : round(lag1),
                lag2 == null ? null : round(lag2),
                rainByYear.containsKey(year) ? round(rainByYear.get(year)) : null,
                (double) (year - 2016)
            ));
        }
        return rows;
    }

    private static Double lastYearYield(List<FeatureRow> all, int holdoutYear) {
        return all.stream()
            .filter(r -> r.harvestYear() == holdoutYear - 1)
            .map(FeatureRow::meanYieldTHa)
            .findFirst()
            .orElse(null);
    }

    private static Double recentMeanOfPrior(List<FeatureRow> all, int holdoutYear, int window) {
        List<Double> vals = all.stream()
            .filter(r -> r.harvestYear() < holdoutYear && r.harvestYear() >= holdoutYear - window)
            .map(FeatureRow::meanYieldTHa)
            .toList();
        if (vals.isEmpty()) {
            return null;
        }
        return vals.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /**
     * OLS: yield = b0 + b1*lag1 + b2*rain (rows missing lag1/rain skipped in train).
     */
    private static Double fitPredictLinear(List<FeatureRow> train, FeatureRow holdout) {
        if (holdout.lag1YieldTHa() == null || holdout.seasonRainMm() == null) {
            // Fallback: lag1-only when rain missing on holdout
            return fitPredictLagOnly(train, holdout);
        }
        List<double[]> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (FeatureRow r : train) {
            if (r.lag1YieldTHa() == null || r.seasonRainMm() == null) {
                continue;
            }
            xs.add(new double[] {1.0, r.lag1YieldTHa(), r.seasonRainMm() / 1000.0});
            ys.add(r.meanYieldTHa());
        }
        if (xs.size() < 3) {
            return fitPredictLagOnly(train, holdout);
        }
        double[] beta = solveNormalEquations(xs, ys, 3);
        if (beta == null) {
            return fitPredictLagOnly(train, holdout);
        }
        return beta[0] + beta[1] * holdout.lag1YieldTHa() + beta[2] * (holdout.seasonRainMm() / 1000.0);
    }

    private static Double fitPredictLagOnly(List<FeatureRow> train, FeatureRow holdout) {
        if (holdout.lag1YieldTHa() == null) {
            return null;
        }
        List<double[]> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (FeatureRow r : train) {
            if (r.lag1YieldTHa() == null) {
                continue;
            }
            xs.add(new double[] {1.0, r.lag1YieldTHa()});
            ys.add(r.meanYieldTHa());
        }
        if (xs.size() < 2) {
            return holdout.lag1YieldTHa();
        }
        double[] beta = solveNormalEquations(xs, ys, 2);
        if (beta == null) {
            return holdout.lag1YieldTHa();
        }
        return beta[0] + beta[1] * holdout.lag1YieldTHa();
    }

    /** Solve X'X b = X'y via Gaussian elimination. */
    private static double[] solveNormalEquations(List<double[]> xs, List<Double> ys, int p) {
        double[][] xtx = new double[p][p];
        double[] xty = new double[p];
        for (int n = 0; n < xs.size(); n++) {
            double[] x = xs.get(n);
            double y = ys.get(n);
            for (int i = 0; i < p; i++) {
                xty[i] += x[i] * y;
                for (int j = 0; j < p; j++) {
                    xtx[i][j] += x[i] * x[j];
                }
            }
        }
        return gaussianEliminate(xtx, xty);
    }

    private static double[] gaussianEliminate(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) {
                    pivot = r;
                }
            }
            if (Math.abs(m[pivot][col]) < 1e-12) {
                return null;
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;
            double div = m[col][col];
            for (int c = col; c <= n; c++) {
                m[col][c] /= div;
            }
            for (int r = 0; r < n; r++) {
                if (r == col) {
                    continue;
                }
                double factor = m[r][col];
                for (int c = col; c <= n; c++) {
                    m[r][c] -= factor * m[col][c];
                }
            }
        }
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = m[i][n];
        }
        return x;
    }

    private static Double absErr(double actual, Double pred) {
        if (pred == null || Double.isNaN(pred)) {
            return null;
        }
        return round(Math.abs(actual - pred));
    }

    private static Double meanAbs(List<Double> values) {
        List<Double> present = values.stream().filter(v -> v != null).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static String bestMethod(Double naive, Double rule, Double linear) {
        record Cand(String name, Double mae) {}
        return List.of(
                new Cand("NAIVE_LAST_YEAR", naive),
                new Cand("RULE_RECENT_MEAN", rule),
                new Cand("LINEAR_LAG_RAIN", linear)
            ).stream()
            .filter(c -> c.mae() != null)
            .min(Comparator.comparingDouble(Cand::mae))
            .map(Cand::name)
            .orElse("NONE");
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
