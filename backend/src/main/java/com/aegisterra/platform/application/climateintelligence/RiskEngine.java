package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.climate.ClimateVariableCodes;
import com.aegisterra.platform.domain.climateintelligence.AlertSeverity;
import com.aegisterra.platform.domain.climateintelligence.AlertType;
import com.aegisterra.platform.domain.climateintelligence.ClimateAlertStatus;
import com.aegisterra.platform.domain.climateintelligence.IndicatorType;
import com.aegisterra.platform.domain.climateintelligence.RiskGrade;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.RiskScoreEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.RiskScoreRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateAlertEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateAlertRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIndicatorRunEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIndicatorRunRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateRuleSetEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateRuleSetRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.DistrictRiskSnapshotEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.DistrictRiskSnapshotRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.FarmClimateProfileEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.FarmClimateProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskEngine {

    private final ClimateDataReadPort climateData;
    private final ClimateRuleSetRepository ruleSetRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final ClimateIndicatorRunRepository indicatorRunRepository;
    private final ClimateAlertRepository alertRepository;
    private final FarmClimateProfileRepository profileRepository;
    private final DistrictRiskSnapshotRepository districtRiskSnapshotRepository;
    private final FarmRepository farmRepository;
    private final ClimateAlertNumberGenerator numberGenerator;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;

    public RiskEngine(
        ClimateDataReadPort climateData,
        ClimateRuleSetRepository ruleSetRepository,
        RiskScoreRepository riskScoreRepository,
        ClimateIndicatorRunRepository indicatorRunRepository,
        ClimateAlertRepository alertRepository,
        FarmClimateProfileRepository profileRepository,
        DistrictRiskSnapshotRepository districtRiskSnapshotRepository,
        FarmRepository farmRepository,
        ClimateAlertNumberGenerator numberGenerator,
        EventBus eventBus,
        ObjectMapper objectMapper
    ) {
        this.climateData = climateData;
        this.ruleSetRepository = ruleSetRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.indicatorRunRepository = indicatorRunRepository;
        this.alertRepository = alertRepository;
        this.profileRepository = profileRepository;
        this.districtRiskSnapshotRepository = districtRiskSnapshotRepository;
        this.farmRepository = farmRepository;
        this.numberGenerator = numberGenerator;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RiskScoreEntity recalculateFarm(UUID farmId, Instant from, Instant to, UUID actorId) {
        ClimateRuleSetEntity rules = ruleSetRepository
            .findFirstByCodeAndActiveTrueAndDeletedFalseOrderByRuleVersionDesc("CLIMATE_RISK_V1")
            .orElseThrow();
        Weights weights = parseWeights(rules.getWeightsJson());
        Thresholds thresholds = parseThresholds(rules.getThresholdsJson());

        Instant windowEnd = to == null ? Instant.now() : to;
        Instant windowStart = from == null ? windowEnd.minus(90, ChronoUnit.DAYS) : from;

        WeatherStationEntity station = nearestStation();
        BigDecimal rain = station == null
            ? BigDecimal.ZERO
            : climateData.rainfallTotal(station.getId(), windowStart, windowEnd);
        List<WeatherObservationEntity> obs = station == null
            ? List.of()
            : climateData.observations(station.getId(), windowStart, windowEnd);

        double droughtNorm = droughtNorm(rain, obs, thresholds);
        double floodNorm = floodNorm(obs, thresholds);
        double rainfallStress = rainfallStress(rain, thresholds);
        double vegetationStress = 0.2; // no VI inputs → mild default, confidence reduced

        double score = 100.0 * clamp(
            weights.drought * droughtNorm
                + weights.flood * floodNorm
                + weights.rainfall * rainfallStress
                + weights.vegetation * vegetationStress
        );
        double confidence = station == null || obs.isEmpty() ? 0.25 : Math.min(1.0, obs.size() / 20.0);
        RiskGrade grade = obs.isEmpty() ? RiskGrade.INSUFFICIENT_DATA : RiskGrade.fromScore(score);

        persistIndicator(IndicatorType.DROUGHT, "FARM", farmId.toString(), windowStart, windowEnd,
            droughtNorm, severityFromNorm(droughtNorm), rules, actorId);
        persistIndicator(IndicatorType.FLOOD, "FARM", farmId.toString(), windowStart, windowEnd,
            floodNorm, severityFromNorm(floodNorm), rules, actorId);
        persistIndicator(IndicatorType.RAINFALL, "FARM", farmId.toString(), windowStart, windowEnd,
            rainfallStress, severityFromNorm(rainfallStress), rules, actorId);

        if (droughtNorm >= thresholds.droughtWarning) {
            raiseAlert(AlertType.DROUGHT, droughtNorm >= thresholds.droughtSevere
                    ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                "FARM", farmId.toString(), farmId, null, rules, Map.of("droughtNorm", droughtNorm), actorId);
        }
        if (floodNorm >= 0.5) {
            raiseAlert(AlertType.FLOOD, floodNorm >= 0.8 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                "FARM", farmId.toString(), farmId, null, rules, Map.of("floodNorm", floodNorm), actorId);
        }

        FarmEntity farm = farmRepository.findByIdAndDeletedFalse(farmId).orElse(null);
        String districtCode = farm == null || farm.getDistrictId() == null
            ? (station == null ? null : station.getDistrictCode())
            : farm.getDistrictId().toString();

        RiskScoreEntity scoreEntity = new RiskScoreEntity();
        scoreEntity.setFarmId(farmId);
        scoreEntity.setScore(BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP));
        scoreEntity.setConfidence(BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP));
        scoreEntity.setModelVersion(rules.getCode() + "@" + rules.getRuleVersion());
        scoreEntity.setCalculatedAt(Instant.now());
        scoreEntity.setGrade(grade.name());
        scoreEntity.setWindowStart(windowStart);
        scoreEntity.setWindowEnd(windowEnd);
        scoreEntity.setRuleSetCode(rules.getCode());
        scoreEntity.setRuleVersion(rules.getRuleVersion());
        scoreEntity.setSubjectType("FARM");
        scoreEntity.setDistrictCode(districtCode);
        try {
            scoreEntity.setComponentsJson(objectMapper.writeValueAsString(Map.of(
                "drought", droughtNorm,
                "flood", floodNorm,
                "rainfall", rainfallStress,
                "vegetation", vegetationStress,
                "rainfallTotalMm", rain
            )));
        } catch (Exception e) {
            scoreEntity.setComponentsJson("{}");
        }
        scoreEntity.setCreatedBy(actorId);
        scoreEntity.setUpdatedBy(actorId);
        scoreEntity.setDeleted(false);
        scoreEntity.setStatus("ACTIVE");
        scoreEntity = riskScoreRepository.save(scoreEntity);

        FarmClimateProfileEntity profile = new FarmClimateProfileEntity();
        profile.setFarmId(farmId);
        profile.setRuleSetCode(rules.getCode());
        profile.setRuleVersion(rules.getRuleVersion());
        profile.setGeneratedAt(Instant.now());
        try {
            profile.setProfileJson(objectMapper.writeValueAsString(Map.of(
                "rainfallTotalMm", rain,
                "observationCount", obs.size(),
                "latestScore", score,
                "grade", grade.name(),
                "windowStart", windowStart.toString(),
                "windowEnd", windowEnd.toString()
            )));
        } catch (Exception e) {
            profile.setProfileJson("{}");
        }
        profile.setCreatedBy(actorId);
        profile.setUpdatedBy(actorId);
        profile.setDeleted(false);
        profile.setStatus("ACTIVE");
        profileRepository.save(profile);

        if (districtCode != null) {
            upsertDistrict(districtCode, scoreEntity, actorId, rules, windowStart, windowEnd);
        }

        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.FARM_RISK_SCORE_CALCULATED,
            actorId,
            "FARM",
            farmId,
            scoreEntity.getId().toString(),
            Map.of("score", score, "grade", grade.name())
        ));
        return scoreEntity;
    }

    private void upsertDistrict(
        String districtCode,
        RiskScoreEntity farmScore,
        UUID actorId,
        ClimateRuleSetEntity rules,
        Instant windowStart,
        Instant windowEnd
    ) {
        List<RiskScoreEntity> districtScores = riskScoreRepository.findByDistrictCodeAndDeletedFalse(districtCode);
        double mean = districtScores.stream().mapToDouble(s -> s.getScore().doubleValue()).average().orElse(
            farmScore.getScore().doubleValue()
        );
        long openAlerts = alertRepository.countByStatusAndDeletedFalse(ClimateAlertStatus.OPEN.name());
        DistrictRiskSnapshotEntity snap = new DistrictRiskSnapshotEntity();
        snap.setDistrictCode(districtCode);
        snap.setScore(BigDecimal.valueOf(mean).setScale(6, RoundingMode.HALF_UP));
        snap.setGrade(RiskGrade.fromScore(mean).name());
        snap.setConfidence(farmScore.getConfidence());
        snap.setFarmCount(Math.max(1, districtScores.size()));
        snap.setOpenAlertCount((int) Math.min(Integer.MAX_VALUE, openAlerts));
        snap.setWindowStart(windowStart);
        snap.setWindowEnd(windowEnd);
        snap.setRuleSetCode(rules.getCode());
        snap.setRuleVersion(rules.getRuleVersion());
        snap.setCalculatedAt(Instant.now());
        snap.setComponentsJson(farmScore.getComponentsJson());
        snap.setCreatedBy(actorId);
        snap.setUpdatedBy(actorId);
        snap.setDeleted(false);
        snap.setStatus("ACTIVE");
        districtRiskSnapshotRepository.save(snap);
    }

    private WeatherStationEntity nearestStation() {
        List<WeatherStationEntity> stations = climateData.stations();
        return stations.isEmpty() ? null : stations.getFirst();
    }

    private double droughtNorm(BigDecimal rain, List<WeatherObservationEntity> obs, Thresholds t) {
        long dryDays = obs.stream()
            .filter(o -> ClimateVariableCodes.RAIN_MM.equals(o.getVariableCode()))
            .filter(o -> o.getValue() != null && o.getValue().doubleValue() < 1.0)
            .count();
        double deficit = rain.doubleValue() < 50 ? (50 - rain.doubleValue()) / 50.0 : 0;
        return clamp(Math.max(deficit, dryDays / 30.0));
    }

    private double floodNorm(List<WeatherObservationEntity> obs, Thresholds t) {
        double peak = obs.stream()
            .filter(o -> ClimateVariableCodes.RAIN_MM.equals(o.getVariableCode()))
            .map(WeatherObservationEntity::getValue)
            .filter(v -> v != null)
            .mapToDouble(BigDecimal::doubleValue)
            .max()
            .orElse(0);
        if (peak >= t.floodCriticalMm24h) {
            return 1.0;
        }
        if (peak >= t.floodWarningMm24h) {
            return 0.75;
        }
        if (peak >= t.floodWatchMm24h) {
            return 0.45;
        }
        return clamp(peak / Math.max(t.floodWatchMm24h, 1));
    }

    private double rainfallStress(BigDecimal rain, Thresholds t) {
        double expected = 100.0;
        double ratio = rain.doubleValue() / expected;
        if (ratio < t.rainfallDeficitPct) {
            return clamp(1.0 - ratio);
        }
        if (ratio > 2.0) {
            return clamp((ratio - 2.0) / 2.0);
        }
        return 0.1;
    }

    private void persistIndicator(
        IndicatorType type,
        String subjectType,
        String subjectId,
        Instant from,
        Instant to,
        double value,
        String severity,
        ClimateRuleSetEntity rules,
        UUID actorId
    ) {
        ClimateIndicatorRunEntity run = new ClimateIndicatorRunEntity();
        run.setIndicatorType(type.name());
        run.setSubjectType(subjectType);
        run.setSubjectId(subjectId);
        run.setWindowStart(from);
        run.setWindowEnd(to);
        run.setIndexValue(BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP));
        run.setSeverity(severity);
        run.setRuleSetCode(rules.getCode());
        run.setRuleVersion(rules.getRuleVersion());
        run.setConfidence(BigDecimal.valueOf(0.7));
        run.setCalculatedAt(Instant.now());
        run.setCreatedBy(actorId);
        run.setMetricsJson("{\"index\":" + value + "}");
        indicatorRunRepository.save(run);
    }

    private void raiseAlert(
        AlertType type,
        AlertSeverity severity,
        String scopeType,
        String scopeId,
        UUID farmId,
        String districtCode,
        ClimateRuleSetEntity rules,
        Map<String, Object> evidence,
        UUID actorId
    ) {
        Optional<ClimateAlertEntity> existing = alertRepository
            .findFirstByAlertTypeAndScopeTypeAndScopeIdAndStatusInAndDeletedFalse(
                type.name(),
                scopeType,
                scopeId,
                List.of(ClimateAlertStatus.OPEN.name(), ClimateAlertStatus.ACKNOWLEDGED.name())
            );
        if (existing.isPresent()) {
            return;
        }
        ClimateAlertEntity alert = new ClimateAlertEntity();
        alert.setAlertNumber(numberGenerator.nextAlert());
        alert.setAlertType(type.name());
        alert.setSeverity(severity.name());
        alert.setScopeType(scopeType);
        alert.setScopeId(scopeId);
        alert.setFarmId(farmId);
        alert.setDistrictCode(districtCode);
        alert.setTitle(type.name() + " " + severity.name());
        alert.setMessage("Deterministic climate alert from rule set " + rules.getCode());
        alert.setValidFrom(Instant.now());
        alert.setRuleSetCode(rules.getCode());
        alert.setRuleVersion(rules.getRuleVersion());
        try {
            alert.setEvidenceJson(objectMapper.writeValueAsString(evidence));
        } catch (Exception e) {
            alert.setEvidenceJson("{}");
        }
        alert.setStatus(ClimateAlertStatus.OPEN.name());
        alert.setCreatedBy(actorId);
        alert.setUpdatedBy(actorId);
        alert.setDeleted(false);
        alert = alertRepository.save(alert);
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.CLIMATE_ALERT_RAISED,
            actorId,
            "CLIMATE_ALERT",
            alert.getId(),
            alert.getAlertNumber(),
            Map.of("alertType", type.name(), "severity", severity.name())
        ));
    }

    private static String severityFromNorm(double norm) {
        if (norm >= 0.7) {
            return AlertSeverity.CRITICAL.name();
        }
        if (norm >= 0.5) {
            return AlertSeverity.WARNING.name();
        }
        if (norm >= 0.3) {
            return AlertSeverity.WATCH.name();
        }
        return AlertSeverity.INFO.name();
    }

    private Weights parseWeights(String json) {
        try {
            JsonNode n = objectMapper.readTree(json);
            return new Weights(
                n.path("drought").asDouble(0.35),
                n.path("flood").asDouble(0.25),
                n.path("rainfall").asDouble(0.20),
                n.path("vegetation").asDouble(0.20)
            );
        } catch (Exception e) {
            return new Weights(0.35, 0.25, 0.20, 0.20);
        }
    }

    private Thresholds parseThresholds(String json) {
        try {
            JsonNode n = objectMapper.readTree(json == null ? "{}" : json);
            return new Thresholds(
                n.path("droughtWatch").asDouble(0.3),
                n.path("droughtWarning").asDouble(0.5),
                n.path("droughtSevere").asDouble(0.7),
                n.path("floodWatchMm24h").asDouble(40),
                n.path("floodWarningMm24h").asDouble(80),
                n.path("floodCriticalMm24h").asDouble(120),
                n.path("rainfallDeficitPct").asDouble(0.4)
            );
        } catch (Exception e) {
            return new Thresholds(0.3, 0.5, 0.7, 40, 80, 120, 0.4);
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private record Weights(double drought, double flood, double rainfall, double vegetation) {}

    private record Thresholds(
        double droughtWatch,
        double droughtWarning,
        double droughtSevere,
        double floodWatchMm24h,
        double floodWarningMm24h,
        double floodCriticalMm24h,
        double rainfallDeficitPct
    ) {}
}
