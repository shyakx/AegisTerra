package com.aegisterra.platform.application.climate;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.climate.ClimateImportJobStatus;
import com.aegisterra.platform.domain.climate.ClimateVariableCodes;
import com.aegisterra.platform.domain.climate.QualityFlag;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateImportJobEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateImportJobRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateQualityReportEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateQualityReportRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateValidationResultEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateValidationResultRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationRepository;
import com.aegisterra.platform.application.contracts.ClimateImportJobResponse;
import com.aegisterra.platform.application.contracts.CreateClimateImportJobRequest;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClimateImportService {

    private final ClimateProviderRegistry providerRegistry;
    private final ClimateImportJobRepository jobRepository;
    private final WeatherStationRepository stationRepository;
    private final WeatherObservationRepository observationRepository;
    private final ClimateValidationResultRepository validationRepository;
    private final ClimateQualityReportRepository qualityReportRepository;
    private final ClimateImportNumberGenerator numberGenerator;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;

    public ClimateImportService(
        ClimateProviderRegistry providerRegistry,
        ClimateImportJobRepository jobRepository,
        WeatherStationRepository stationRepository,
        WeatherObservationRepository observationRepository,
        ClimateValidationResultRepository validationRepository,
        ClimateQualityReportRepository qualityReportRepository,
        ClimateImportNumberGenerator numberGenerator,
        EventBus eventBus,
        ObjectMapper objectMapper
    ) {
        this.providerRegistry = providerRegistry;
        this.jobRepository = jobRepository;
        this.stationRepository = stationRepository;
        this.observationRepository = observationRepository;
        this.validationRepository = validationRepository;
        this.qualityReportRepository = qualityReportRepository;
        this.numberGenerator = numberGenerator;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateImportJobResponse> list(String status, Pageable pageable) {
        var page = status == null || status.isBlank()
            ? jobRepository.findByDeletedFalse(pageable)
            : jobRepository.findByStatusAndDeletedFalse(status.trim(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClimateImportJobResponse get(UUID id) {
        return toResponse(jobRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import job not found")));
    }

    @Transactional
    public ClimateImportJobResponse start(CreateClimateImportJobRequest request, UUID actorId) {
        providerRegistry.require(request.providerCode());
        if (!"MANUAL".equalsIgnoreCase(request.providerCode())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Only MANUAL provider imports are enabled in Phase 8A");
        }
        if (request.stationCode() == null || request.stationCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stationCode is required");
        }
        WeatherStationEntity station = stationRepository.findByCodeAndDeletedFalse(request.stationCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));

        ClimateImportJobEntity job = new ClimateImportJobEntity();
        job.setJobNumber(numberGenerator.next());
        job.setProviderCode(request.providerCode().toUpperCase(Locale.ROOT));
        job.setJobType(request.jobType() == null || request.jobType().isBlank() ? "MANUAL" : request.jobType());
        job.setStatus(ClimateImportJobStatus.RUNNING.name());
        job.setStartedAt(Instant.now());
        job.setCreatedBy(actorId);
        job.setUpdatedBy(actorId);
        job.setDeleted(false);
        job = jobRepository.save(job);

        List<RawObservation> rows = new ArrayList<>();
        if (request.observations() != null) {
            for (var row : request.observations()) {
                rows.add(new RawObservation(row.observedAt(), row.variableCode(), row.value(), row.unit()));
            }
        }
        if (request.csvContent() != null && !request.csvContent().isBlank()) {
            rows.addAll(parseCsv(request.csvContent()));
        }
        if (rows.isEmpty()) {
            job.setStatus(ClimateImportJobStatus.FAILED.name());
            job.setErrorSummary("No observation rows provided");
            job.setCompletedAt(Instant.now());
            return toResponse(jobRepository.save(job));
        }

        int accepted = 0;
        int rejected = 0;
        Instant min = null;
        Instant max = null;
        for (RawObservation raw : rows) {
            job.setRowsRead(job.getRowsRead() + 1);
            ValidationOutcome outcome = validate(raw, station);
            if (!outcome.valid()) {
                rejected++;
                saveValidation(job.getId(), actorId, outcome.ruleCode(), "ERROR", outcome.message(), null);
                continue;
            }
            String hash = hash(station.getId(), raw.observedAt(), raw.variableCode(), raw.value());
            WeatherObservationEntity obs = new WeatherObservationEntity();
            obs.setStationId(station.getId());
            obs.setObservedAt(raw.observedAt());
            String variable = ClimateVariableCodes.canonical(raw.variableCode());
            obs.setVariableCode(variable);
            obs.setValue(raw.value());
            obs.setUnit(raw.unit() == null
                ? ClimateVariableCodes.defaultUnit(variable).orElse(null)
                : raw.unit());
            obs.setQualityFlag(QualityFlag.VALID.name());
            obs.setProviderCode(job.getProviderCode());
            obs.setImportJobId(job.getId());
            obs.setSourcePayloadHash(hash);
            obs.setSource("MANUAL");
            obs.setCreatedBy(actorId);
            dualWriteWide(obs, variable, raw.value());
            observationRepository.save(obs);
            accepted++;
            min = min == null || raw.observedAt().isBefore(min) ? raw.observedAt() : min;
            max = max == null || raw.observedAt().isAfter(max) ? raw.observedAt() : max;
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.CLIMATE_OBSERVATION_ACCEPTED,
                actorId,
                "WEATHER_OBSERVATION",
                obs.getId(),
                job.getJobNumber(),
                Map.of("stationId", station.getId().toString(), "variableCode", variable)
            ));
        }

        job.setRowsAccepted(accepted);
        job.setRowsRejected(rejected);
        job.setRequestedWindowStart(min);
        job.setRequestedWindowEnd(max);
        job.setCompletedAt(Instant.now());
        if (accepted == 0) {
            job.setStatus(ClimateImportJobStatus.FAILED.name());
            job.setErrorSummary("All rows rejected");
        } else if (rejected > 0) {
            job.setStatus(ClimateImportJobStatus.PARTIAL.name());
        } else {
            job.setStatus(ClimateImportJobStatus.SUCCEEDED.name());
        }
        job = jobRepository.save(job);

        ClimateQualityReportEntity report = new ClimateQualityReportEntity();
        report.setScopeType("JOB");
        report.setScopeId(job.getId());
        report.setPeriodStart(min);
        report.setPeriodEnd(max);
        report.setOverallGrade(rejected == 0 ? "A" : rejected > accepted ? "D" : "B");
        try {
            report.setMetricsJson(objectMapper.writeValueAsString(Map.of(
                "rowsRead", job.getRowsRead(),
                "rowsAccepted", accepted,
                "rowsRejected", rejected,
                "duplicateRate", 0,
                "completeness", accepted == 0 ? 0 : (double) accepted / job.getRowsRead()
            )));
        } catch (Exception e) {
            report.setMetricsJson("{\"error\":\"metrics\"}");
        }
        report.setCreatedBy(actorId);
        if (report.getGeneratedAt() == null) {
            report.setGeneratedAt(Instant.now());
        }
        report.setDeleted(false);
        report.setStatus("ACTIVE");
        qualityReportRepository.save(report);
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.CLIMATE_QUALITY_REPORT_GENERATED,
            actorId,
            "CLIMATE_QUALITY_REPORT",
            report.getId(),
            job.getJobNumber(),
            Map.of("grade", report.getOverallGrade())
        ));
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.CLIMATE_IMPORT_COMPLETED,
            actorId,
            "CLIMATE_IMPORT_JOB",
            job.getId(),
            job.getJobNumber(),
            Map.of(
                "status", job.getStatus(),
                "stationId", station.getId().toString(),
                "rowsAccepted", accepted,
                "rowsRejected", rejected
            )
        ));
        return toResponse(job);
    }

    private void dualWriteWide(WeatherObservationEntity obs, String variable, BigDecimal value) {
        switch (variable) {
            case ClimateVariableCodes.TEMP_C -> obs.setTemperatureC(value);
            case ClimateVariableCodes.RAIN_MM -> obs.setRainfallMm(value);
            case ClimateVariableCodes.HUMIDITY_PCT -> obs.setHumidityPct(value);
            case ClimateVariableCodes.WIND_MS -> obs.setWindSpeedMs(value);
            default -> {
            }
        }
    }

    private ValidationOutcome validate(RawObservation raw, WeatherStationEntity station) {
        if (raw.observedAt() == null) {
            return ValidationOutcome.fail("OBS_TIME_REQUIRED", "observed_at is required");
        }
        if (raw.observedAt().isAfter(Instant.now().plusSeconds(3600))) {
            return ValidationOutcome.fail("OBS_TIME_SKEW", "observed_at is too far in the future");
        }
        if (raw.variableCode() == null || raw.variableCode().isBlank()) {
            return ValidationOutcome.fail("UNIT_KNOWN", "variable_code is required");
        }
        if (raw.value() == null) {
            return ValidationOutcome.fail("VALUE_RANGE", "value is required");
        }
        if (!ClimateVariableCodes.inPhysicalRange(raw.variableCode(), raw.value())) {
            return ValidationOutcome.fail("VALUE_RANGE", "value outside physical bounds");
        }
        if (station == null || station.isDeleted()) {
            return ValidationOutcome.fail("STATION_EXISTS", "station inactive");
        }
        return ValidationOutcome.ok();
    }

    private void saveValidation(UUID jobId, UUID actorId, String rule, String severity, String message, UUID subjectId) {
        ClimateValidationResultEntity v = new ClimateValidationResultEntity();
        v.setImportJobId(jobId);
        v.setSubjectType("OBSERVATION");
        v.setSubjectId(subjectId);
        v.setRuleCode(rule);
        v.setSeverity(severity);
        v.setMessage(message);
        v.setOccurredAt(Instant.now());
        v.setCreatedBy(actorId);
        validationRepository.save(v);
    }

    private List<RawObservation> parseCsv(String csv) {
        String[] lines = csv.split("\\R");
        if (lines.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV must include header and rows");
        }
        String[] header = lines[0].split(",");
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            idx.put(header[i].trim().toLowerCase(Locale.ROOT), i);
        }
        if (!idx.containsKey("observed_at") || !idx.containsKey("variable_code") || !idx.containsKey("value")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "CSV requires observed_at,variable_code,value columns");
        }
        List<RawObservation> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] parts = lines[i].split(",", -1);
            Instant observedAt = Instant.parse(parts[idx.get("observed_at")].trim());
            String variable = parts[idx.get("variable_code")].trim();
            BigDecimal value = new BigDecimal(parts[idx.get("value")].trim());
            String unit = idx.containsKey("unit") ? parts[idx.get("unit")].trim() : null;
            rows.add(new RawObservation(observedAt, variable, value, unit));
        }
        return rows;
    }

    private static String hash(UUID stationId, Instant observedAt, String variable, BigDecimal value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = stationId + "|" + observedAt + "|" + variable + "|" + value;
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public ClimateImportJobResponse toResponse(ClimateImportJobEntity job) {
        return new ClimateImportJobResponse(
            job.getId(),
            job.getJobNumber(),
            job.getProviderCode(),
            job.getJobType(),
            job.getStatus(),
            job.getRowsRead(),
            job.getRowsAccepted(),
            job.getRowsRejected(),
            job.getStartedAt(),
            job.getCompletedAt(),
            job.getErrorSummary(),
            job.getCreatedAt()
        );
    }

    private record RawObservation(Instant observedAt, String variableCode, BigDecimal value, String unit) {}

    private record ValidationOutcome(boolean valid, String ruleCode, String message) {
        static ValidationOutcome ok() { return new ValidationOutcome(true, null, null); }
        static ValidationOutcome fail(String rule, String message) { return new ValidationOutcome(false, rule, message); }
    }
}
