package com.aegisterra.platform.application.climate;

import com.aegisterra.platform.infrastructure.persistence.climate.ClimateDatasetEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateDatasetRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateImportJobRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateProviderEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateProviderRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateQualityReportEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateQualityReportRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateRasterLayerEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateRasterLayerRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.SatelliteObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.SatelliteObservationRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationRepository;
import com.aegisterra.platform.application.contracts.ClimateDashboardResponse;
import com.aegisterra.platform.application.contracts.ClimateDatasetResponse;
import com.aegisterra.platform.application.contracts.ClimateProviderResponse;
import com.aegisterra.platform.application.contracts.ClimateQualityReportResponse;
import com.aegisterra.platform.application.contracts.ClimateRasterLayerResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.SatelliteObservationResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
public class ClimateCatalogService {

    private final ClimateProviderRepository providerRepository;
    private final WeatherStationRepository stationRepository;
    private final WeatherObservationRepository observationRepository;
    private final ClimateImportJobRepository importJobRepository;
    private final ClimateQualityReportRepository qualityReportRepository;
    private final ClimateDatasetRepository datasetRepository;
    private final ClimateRasterLayerRepository rasterLayerRepository;
    private final SatelliteObservationRepository satelliteObservationRepository;

    public ClimateCatalogService(
        ClimateProviderRepository providerRepository,
        WeatherStationRepository stationRepository,
        WeatherObservationRepository observationRepository,
        ClimateImportJobRepository importJobRepository,
        ClimateQualityReportRepository qualityReportRepository,
        ClimateDatasetRepository datasetRepository,
        ClimateRasterLayerRepository rasterLayerRepository,
        SatelliteObservationRepository satelliteObservationRepository
    ) {
        this.providerRepository = providerRepository;
        this.stationRepository = stationRepository;
        this.observationRepository = observationRepository;
        this.importJobRepository = importJobRepository;
        this.qualityReportRepository = qualityReportRepository;
        this.datasetRepository = datasetRepository;
        this.rasterLayerRepository = rasterLayerRepository;
        this.satelliteObservationRepository = satelliteObservationRepository;
    }

    @Transactional(readOnly = true)
    public ClimateDashboardResponse dashboard() {
        Instant recent = Instant.now().minus(30, ChronoUnit.DAYS);
        String grade = qualityReportRepository.findFirstByOrderByGeneratedAtDesc()
            .map(ClimateQualityReportEntity::getOverallGrade)
            .orElse(null);
        return new ClimateDashboardResponse(
            providerRepository.countByDeletedFalse(),
            providerRepository.countByEnabledTrueAndDeletedFalse(),
            stationRepository.countByDeletedFalse(),
            observationRepository.countByObservedAtGreaterThanEqual(recent),
            importJobRepository.countByStatusInAndDeletedFalse(List.of("PENDING", "RUNNING")),
            grade
        );
    }

    @Transactional(readOnly = true)
    public List<ClimateProviderResponse> providers() {
        return providerRepository.findByDeletedFalseOrderByCodeAsc().stream().map(this::toProvider).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateDatasetResponse> datasets(String status, Pageable pageable) {
        var page = status == null || status.isBlank()
            ? datasetRepository.findByDeletedFalse(pageable)
            : datasetRepository.findByStatusAndDeletedFalse(status, pageable);
        return PageResponse.from(page.map(this::toDataset));
    }

    @Transactional(readOnly = true)
    public ClimateDatasetResponse dataset(UUID id) {
        return toDataset(datasetRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found")));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateRasterLayerResponse> rasters(Pageable pageable) {
        return PageResponse.from(rasterLayerRepository.findByDeletedFalse(pageable).map(this::toRaster));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClimateQualityReportResponse> qualityReports(Pageable pageable) {
        return PageResponse.from(qualityReportRepository.findAllByOrderByGeneratedAtDesc(pageable).map(this::toQuality));
    }

    @Transactional(readOnly = true)
    public PageResponse<SatelliteObservationResponse> satellite(Instant from, Instant to, UUID farmId, Pageable pageable) {
        ClimateObservationService.requireWindow(from, to);
        var page = farmId == null
            ? satelliteObservationRepository.findByObservedAtBetween(from, to, pageable)
            : satelliteObservationRepository.findByFarmIdAndObservedAtBetween(farmId, from, to, pageable);
        return PageResponse.from(page.map(this::toSatellite));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mapStations() {
        List<Map<String, Object>> features = new ArrayList<>();
        for (WeatherStationEntity s : stationRepository.findByDeletedFalse()) {
            if (s.getGeom() == null) {
                continue;
            }
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", Map.of(
                "type", "Point",
                "coordinates", List.of(s.getGeom().getX(), s.getGeom().getY())
            ));
            feature.put("properties", Map.of(
                "id", s.getId().toString(),
                "code", s.getCode(),
                "name", s.getName(),
                "providerCode", s.getProviderCode() == null ? "" : s.getProviderCode()
            ));
            features.add(feature);
        }
        return Map.of("type", "FeatureCollection", "features", features);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mapFootprints() {
        List<Map<String, Object>> features = new ArrayList<>();
        for (SatelliteObservationEntity s : satelliteObservationRepository.findByFootprintIsNotNull()) {
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", Map.of("type", "MultiPolygon", "coordinates", List.of()));
            feature.put("properties", Map.of(
                "id", s.getId().toString(),
                "productId", s.getProductId(),
                "providerCode", s.getProviderCode() == null ? "" : s.getProviderCode()
            ));
            features.add(feature);
        }
        return Map.of("type", "FeatureCollection", "features", features);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report(String name) {
        return switch (name) {
            case "by-provider" -> Map.of(
                "reportCode", "CLIMATE_BY_PROVIDER",
                "providers", providers()
            );
            case "quality" -> Map.of(
                "reportCode", "CLIMATE_QUALITY",
                "latestGrade", qualityReportRepository.findFirstByOrderByGeneratedAtDesc()
                    .map(ClimateQualityReportEntity::getOverallGrade).orElse(null)
            );
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
    }

    private ClimateProviderResponse toProvider(ClimateProviderEntity p) {
        return new ClimateProviderResponse(
            p.getId(), p.getCode(), p.getDisplayName(), p.isEnabled(), p.getCapabilitiesJson(), p.getStatus()
        );
    }

    private ClimateDatasetResponse toDataset(ClimateDatasetEntity d) {
        return new ClimateDatasetResponse(
            d.getId(), d.getCode(), d.getName(), d.getDescription(), d.getDatasetType(),
            d.getProviderCode(), d.getStatus(), d.getTimeStart(), d.getTimeEnd()
        );
    }

    private ClimateRasterLayerResponse toRaster(ClimateRasterLayerEntity r) {
        return new ClimateRasterLayerResponse(
            r.getId(), r.getCode(), r.getName(), r.getProviderCode(), r.getVariableCode(),
            r.getStorageUri(), r.getFormat(), r.getStatus()
        );
    }

    private ClimateQualityReportResponse toQuality(ClimateQualityReportEntity q) {
        return new ClimateQualityReportResponse(
            q.getId(), q.getScopeType(), q.getScopeId() == null ? null : q.getScopeId().toString(),
            q.getPeriodStart(), q.getPeriodEnd(), q.getMetricsJson(), q.getOverallGrade(), q.getGeneratedAt()
        );
    }

    private SatelliteObservationResponse toSatellite(SatelliteObservationEntity s) {
        return new SatelliteObservationResponse(
            s.getId(), s.getFarmId(), s.getProductId(), s.getProductVersion(), s.getObservedAt(),
            s.getCloudCoverPct(), s.getStorageUri(), s.getProviderCode(), s.getQualityFlag()
        );
    }
}
