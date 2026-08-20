package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.domain.climate.ClimateVariableCodes;
import com.aegisterra.platform.domain.climateintelligence.ClimateAlertStatus;
import com.aegisterra.platform.infrastructure.persistence.climate.RiskScoreEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.RiskScoreRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateAlertEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateAlertRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIndicatorRunEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIndicatorRunRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIntelJobEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.ClimateIntelJobRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.DistrictRiskSnapshotEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.DistrictRiskSnapshotRepository;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.FarmClimateProfileEntity;
import com.aegisterra.platform.infrastructure.persistence.climateintelligence.FarmClimateProfileRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.application.contracts.ClimateAlertResponse;
import com.aegisterra.platform.application.contracts.ClimateIndicatorResponse;
import com.aegisterra.platform.application.contracts.ClimateIntelJobResponse;
import com.aegisterra.platform.application.contracts.ClimateTimelineItemResponse;
import com.aegisterra.platform.application.contracts.DistrictRiskProfileResponse;
import com.aegisterra.platform.application.contracts.FarmClimateProfileResponse;
import com.aegisterra.platform.application.contracts.FarmRiskScoreResponse;
import com.aegisterra.platform.application.contracts.NationalRiskDashboardResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.RecalculateClimateIntelRequest;
import com.aegisterra.platform.application.contracts.SeasonSummaryResponse;
import com.aegisterra.platform.application.contracts.WeatherSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClimateIntelligenceService {

    private final RiskEngine riskEngine;
    private final RiskScoreRepository riskScoreRepository;
    private final FarmClimateProfileRepository profileRepository;
    private final ClimateAlertRepository alertRepository;
    private final ClimateIndicatorRunRepository indicatorRunRepository;
    private final DistrictRiskSnapshotRepository districtRiskSnapshotRepository;
    private final ClimateIntelJobRepository intelJobRepository;
    private final ClimateAlertNumberGenerator numberGenerator;
    private final ClimateDataReadPort climateData;
    private final FarmRepository farmRepository;

    public ClimateIntelligenceService(
        RiskEngine riskEngine,
        RiskScoreRepository riskScoreRepository,
        FarmClimateProfileRepository profileRepository,
        ClimateAlertRepository alertRepository,
        ClimateIndicatorRunRepository indicatorRunRepository,
        DistrictRiskSnapshotRepository districtRiskSnapshotRepository,
        ClimateIntelJobRepository intelJobRepository,
        ClimateAlertNumberGenerator numberGenerator,
        ClimateDataReadPort climateData,
        FarmRepository farmRepository
    ) {
        this.riskEngine = riskEngine;
        this.riskScoreRepository = riskScoreRepository;
        this.profileRepository = profileRepository;
        this.alertRepository = alertRepository;
        this.indicatorRunRepository = indicatorRunRepository;
        this.districtRiskSnapshotRepository = districtRiskSnapshotRepository;
        this.intelJobRepository = intelJobRepository;
        this.numberGenerator = numberGenerator;
        this.climateData = climateData;
        this.farmRepository = farmRepository;
    }

    @Transactional(readOnly = true)
    public NationalRiskDashboardResponse nationalDashboard() {
        Map<String, Integer> byGrade = new HashMap<>();
        for (RiskScoreEntity score : riskScoreRepository.findByDeletedFalseAndFarmIdIsNotNull()) {
            String grade = score.getGrade() == null ? "UNKNOWN" : score.getGrade();
            byGrade.merge(grade, 1, Integer::sum);
        }
        List<NationalRiskDashboardResponse.DistrictHeat> heat = districtRiskSnapshotRepository
            .findByDeletedFalseOrderByScoreDesc().stream()
            .map(d -> new NationalRiskDashboardResponse.DistrictHeat(
                d.getDistrictCode(),
                d.getScore() == null ? null : d.getScore().doubleValue(),
                d.getGrade()
            ))
            .toList();
        long open = alertRepository.countByStatusAndDeletedFalse(ClimateAlertStatus.OPEN.name());
        long critical = alertRepository.countByStatusAndSeverityAndDeletedFalse(
            ClimateAlertStatus.OPEN.name(), "CRITICAL"
        );
        long stations = climateData.stations().size();
        Double coverage = stations == 0 ? 0.0 : Math.min(100.0, stations * 10.0);
        return new NationalRiskDashboardResponse(
            byGrade,
            critical,
            open,
            heat,
            coverage,
            Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public FarmRiskScoreResponse farmRisk(UUID farmId) {
        RiskScoreEntity score = riskScoreRepository.findFirstByFarmIdAndDeletedFalseOrderByCalculatedAtDesc(farmId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk score not found"));
        return toRisk(score);
    }

    @Transactional(readOnly = true)
    public FarmClimateProfileResponse farmProfile(UUID farmId) {
        FarmClimateProfileEntity profile = profileRepository
            .findFirstByFarmIdAndDeletedFalseOrderByGeneratedAtDesc(farmId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        RiskScoreEntity latest = riskScoreRepository.findFirstByFarmIdAndDeletedFalseOrderByCalculatedAtDesc(farmId)
            .orElse(null);
        return new FarmClimateProfileResponse(
            profile.getId(),
            profile.getFarmId(),
            profile.getGeneratedAt(),
            profile.getRuleVersion(),
            profile.getProfileJson(),
            latest == null ? null : latest.getScore().doubleValue(),
            latest == null ? null : latest.getGrade()
        );
    }

    @Transactional(readOnly = true)
    public List<ClimateTimelineItemResponse> timeline(UUID farmId) {
        List<ClimateTimelineItemResponse> items = new ArrayList<>();
        for (ClimateAlertEntity alert : alertRepository.findByFarmIdAndDeletedFalseOrderByValidFromDesc(farmId)) {
            items.add(new ClimateTimelineItemResponse(
                alert.getValidFrom(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getTitle() == null ? alert.getAlertNumber() : alert.getTitle(),
                alert.getMessage(),
                alert.getEvidenceJson()
            ));
        }
        for (ClimateIndicatorRunEntity run : indicatorRunRepository
            .findBySubjectTypeAndSubjectIdOrderByCalculatedAtDesc("FARM", farmId.toString())) {
            items.add(new ClimateTimelineItemResponse(
                run.getCalculatedAt(),
                run.getIndicatorType(),
                run.getSeverity(),
                run.getIndicatorType() + " indicator",
                "index=" + run.getIndexValue(),
                run.getMetricsJson()
            ));
        }
        items.sort((a, b) -> b.occurredAt().compareTo(a.occurredAt()));
        return items;
    }

    @Transactional(readOnly = true)
    public WeatherSummaryResponse weatherSummary(UUID farmId, Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to are required");
        }
        WeatherStationEntity station = climateData.stations().stream().findFirst().orElse(null);
        if (station == null) {
            return new WeatherSummaryResponse(farmId, from, to, 0.0, null, 0, 0,
                "No climate stations available for summary.", "{}");
        }
        List<WeatherObservationEntity> obs = climateData.observations(station.getId(), from, to);
        BigDecimal rain = climateData.rainfallTotal(station.getId(), from, to);
        double tempSum = 0;
        int tempCount = 0;
        int dry = 0;
        int wet = 0;
        for (WeatherObservationEntity o : obs) {
            if (ClimateVariableCodes.TEMP_C.equals(o.getVariableCode()) && o.getValue() != null) {
                tempSum += o.getValue().doubleValue();
                tempCount++;
            }
            if (ClimateVariableCodes.RAIN_MM.equals(o.getVariableCode()) && o.getValue() != null) {
                if (o.getValue().doubleValue() < 1) {
                    dry++;
                } else {
                    wet++;
                }
            }
        }
        Double meanTemp = tempCount == 0 ? null : tempSum / tempCount;
        String narrative = "Period rainfall " + rain + " mm"
            + (meanTemp == null ? "" : (", mean temperature " + BigDecimal.valueOf(meanTemp).setScale(1, RoundingMode.HALF_UP) + " °C"))
            + ".";
        return new WeatherSummaryResponse(
            farmId, from, to, rain.doubleValue(), meanTemp, dry, wet, narrative,
            "{\"stationCode\":\"" + station.getCode() + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public SeasonSummaryResponse seasonSummary(UUID farmId, String seasonCode) {
        RiskScoreEntity latest = riskScoreRepository.findFirstByFarmIdAndDeletedFalseOrderByCalculatedAtDesc(farmId)
            .orElse(null);
        String grade = latest == null ? "INSUFFICIENT_DATA" : latest.getGrade();
        String metrics = latest == null ? "{}" : (latest.getComponentsJson() == null ? "{}" : latest.getComponentsJson());
        return new SeasonSummaryResponse(
            farmId,
            seasonCode == null ? "CURRENT" : seasonCode,
            grade,
            metrics,
            "Season summary derived from latest deterministic risk components.",
            Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public DistrictRiskProfileResponse districtProfile(String code) {
        DistrictRiskSnapshotEntity snap = districtRiskSnapshotRepository
            .findFirstByDistrictCodeAndDeletedFalseOrderByCalculatedAtDesc(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "District profile not found"));
        return new DistrictRiskProfileResponse(
            snap.getDistrictCode(),
            snap.getScore() == null ? null : snap.getScore().doubleValue(),
            snap.getScore() == null ? null : snap.getScore().doubleValue(),
            snap.getFarmCount(),
            snap.getOpenAlertCount(),
            snap.getGrade(),
            snap.getComponentsJson(),
            snap.getCalculatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateAlertResponse> alerts(
        String status, String alertType, String severity, String scopeType, Pageable pageable
    ) {
        return PageResponse.from(alertRepository.search(
            blank(status), blank(alertType), blank(severity), blank(scopeType), pageable
        ).map(this::toAlert));
    }

    @Transactional(readOnly = true)
    public ClimateAlertResponse alert(UUID id) {
        return toAlert(alertRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found")));
    }

    @Transactional
    public ClimateAlertResponse acknowledge(UUID id, UUID actorId) {
        ClimateAlertEntity alert = alertRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        alert.setStatus(ClimateAlertStatus.ACKNOWLEDGED.name());
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(actorId);
        alert.setUpdatedBy(actorId);
        return toAlert(alertRepository.save(alert));
    }

    @Transactional
    public ClimateIntelJobResponse recalculate(RecalculateClimateIntelRequest request, UUID actorId) {
        ClimateIntelJobEntity job = new ClimateIntelJobEntity();
        job.setJobNumber(numberGenerator.nextJob());
        job.setJobType("RECALCULATE");
        job.setStatus("RUNNING");
        job.setStartedAt(Instant.now());
        job.setParamsJson("{\"farmId\":\"" + request.farmId() + "\",\"districtCode\":\"" + request.districtCode() + "\"}");
        job.setCreatedBy(actorId);
        job.setUpdatedBy(actorId);
        job.setDeleted(false);
        job = intelJobRepository.save(job);

        Instant to = request.to() == null ? Instant.now() : request.to();
        Instant from = request.from() == null ? to.minus(90, ChronoUnit.DAYS) : request.from();
        int processed = 0;
        int failed = 0;
        try {
            if (request.farmId() != null) {
                riskEngine.recalculateFarm(request.farmId(), from, to, actorId);
                processed++;
            } else {
                var farms = farmRepository.findAll().stream().filter(f -> !f.isDeleted()).limit(25).toList();
                if (farms.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No farms available for recalculation; provide farmId");
                }
                for (var farm : farms) {
                    try {
                        riskEngine.recalculateFarm(farm.getId(), from, to, actorId);
                        processed++;
                    } catch (Exception e) {
                        failed++;
                    }
                }
            }
            job.setStatus(failed > 0 && processed == 0 ? "FAILED" : "SUCCEEDED");
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorSummary(e.getMessage());
            failed++;
        }
        job.setSubjectsProcessed(processed);
        job.setSubjectsFailed(failed);
        job.setCompletedAt(Instant.now());
        return toJob(intelJobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateIndicatorResponse> indicators(
        String indicatorType, String subjectType, String subjectId, Pageable pageable
    ) {
        if (subjectType != null && subjectId != null) {
            return PageResponse.from(indicatorRunRepository
                .findBySubjectTypeAndSubjectId(subjectType, subjectId, pageable)
                .map(this::toIndicator));
        }
        if (indicatorType != null) {
            return PageResponse.from(indicatorRunRepository.findByIndicatorType(indicatorType, pageable).map(this::toIndicator));
        }
        return PageResponse.from(indicatorRunRepository.findAll(pageable).map(this::toIndicator));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mapRisk() {
        List<Map<String, Object>> features = new ArrayList<>();
        for (RiskScoreEntity score : riskScoreRepository.findByDeletedFalseAndFarmIdIsNotNull()) {
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", null);
            feature.put("properties", Map.of(
                "farmId", score.getFarmId().toString(),
                "score", score.getScore(),
                "grade", score.getGrade() == null ? "" : score.getGrade()
            ));
            features.add(feature);
        }
        return Map.of("type", "FeatureCollection", "features", features);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report(String name) {
        return switch (name) {
            case "FARMS_BY_RISK_GRADE", "farms-by-risk-grade" -> Map.of(
                "reportCode", "FARMS_BY_RISK_GRADE",
                "dashboard", nationalDashboard()
            );
            case "OPEN_ALERTS_BY_TYPE", "open-alerts-by-type" -> Map.of(
                "reportCode", "OPEN_ALERTS_BY_TYPE",
                "openAlerts", alertRepository.countByStatusAndDeletedFalse(ClimateAlertStatus.OPEN.name())
            );
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
    }

    private FarmRiskScoreResponse toRisk(RiskScoreEntity s) {
        return new FarmRiskScoreResponse(
            s.getId(), s.getFarmId(), s.getScore().doubleValue(),
            s.getConfidence() == null ? null : s.getConfidence().doubleValue(),
            s.getGrade(), s.getWindowStart(), s.getWindowEnd(), s.getComponentsJson(),
            s.getRuleSetCode(), s.getRuleVersion(), s.getModelVersion(), s.getCalculatedAt()
        );
    }

    private ClimateAlertResponse toAlert(ClimateAlertEntity a) {
        return new ClimateAlertResponse(
            a.getId(), a.getAlertNumber(), a.getAlertType(), a.getSeverity(), a.getScopeType(), a.getScopeId(),
            a.getValidFrom(), a.getValidTo(), a.getRuleVersion(), a.getEvidenceJson(), a.getStatus(), a.getCreatedAt()
        );
    }

    private ClimateIndicatorResponse toIndicator(ClimateIndicatorRunEntity r) {
        return new ClimateIndicatorResponse(
            r.getId(), r.getIndicatorType(), r.getSubjectType(), r.getSubjectId(), r.getSeverity(),
            r.getIndexValue() == null ? null : r.getIndexValue().doubleValue(),
            r.getWindowStart(), r.getWindowEnd(), r.getMetricsJson(), r.getCalculatedAt()
        );
    }

    private ClimateIntelJobResponse toJob(ClimateIntelJobEntity j) {
        return new ClimateIntelJobResponse(
            j.getId(), j.getJobNumber(), j.getJobType(), j.getStatus(), j.getSubjectsProcessed(),
            j.getErrorSummary(), j.getStartedAt(), j.getCompletedAt(), j.getCreatedAt()
        );
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
