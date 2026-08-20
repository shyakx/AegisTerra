package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.climate.ClimateCatalogService;
import com.aegisterra.platform.application.climate.ClimateImportService;
import com.aegisterra.platform.application.climate.ClimateObservationService;
import com.aegisterra.platform.application.climate.ClimateStationService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.ClimateDashboardResponse;
import com.aegisterra.platform.application.contracts.ClimateDatasetResponse;
import com.aegisterra.platform.application.contracts.ClimateImportJobResponse;
import com.aegisterra.platform.application.contracts.ClimateProviderResponse;
import com.aegisterra.platform.application.contracts.ClimateQualityReportResponse;
import com.aegisterra.platform.application.contracts.ClimateRasterLayerResponse;
import com.aegisterra.platform.application.contracts.CreateClimateImportJobRequest;
import com.aegisterra.platform.application.contracts.CreateWeatherStationRequest;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.SatelliteObservationResponse;
import com.aegisterra.platform.application.contracts.WeatherObservationResponse;
import com.aegisterra.platform.application.contracts.WeatherStationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/climate")
@Tag(name = "Climate Data")
public class ClimateController {

    private final ClimateCatalogService catalogService;
    private final ClimateStationService stationService;
    private final ClimateObservationService observationService;
    private final ClimateImportService importService;

    public ClimateController(
        ClimateCatalogService catalogService,
        ClimateStationService stationService,
        ClimateObservationService observationService,
        ClimateImportService importService
    ) {
        this.catalogService = catalogService;
        this.stationService = stationService;
        this.observationService = observationService;
        this.importService = importService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('climate:read')")
    public ClimateDashboardResponse dashboard() {
        return catalogService.dashboard();
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('climate:read')")
    public List<ClimateProviderResponse> providers() {
        return catalogService.providers();
    }

    @GetMapping("/stations")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<WeatherStationResponse> stations(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return stationService.search(q, status, pageable);
    }

    @GetMapping("/stations/{id}")
    @PreAuthorize("hasAuthority('climate:read')")
    public WeatherStationResponse station(@PathVariable UUID id) {
        return stationService.get(id);
    }

    @PostMapping("/stations")
    @PreAuthorize("hasAuthority('climate:write')")
    public ResponseEntity<WeatherStationResponse> createStation(
        @RequestBody CreateWeatherStationRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationService.create(request, actor.id()));
    }

    @GetMapping("/observations")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<WeatherObservationResponse> observations(
        @RequestParam(required = false) UUID stationId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) String variableCode,
        @RequestParam(required = false) String qualityFlag,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        return observationService.search(stationId, from, to, variableCode, qualityFlag, pageable);
    }

    @GetMapping("/satellite-observations")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<SatelliteObservationResponse> satellite(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) UUID farmId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return catalogService.satellite(from, to, farmId, pageable);
    }

    @GetMapping("/raster-layers")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<ClimateRasterLayerResponse> rasters(@PageableDefault(size = 20) Pageable pageable) {
        return catalogService.rasters(pageable);
    }

    @GetMapping("/datasets")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<ClimateDatasetResponse> datasets(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return catalogService.datasets(status, pageable);
    }

    @GetMapping("/datasets/{id}")
    @PreAuthorize("hasAuthority('climate:read')")
    public ClimateDatasetResponse dataset(@PathVariable UUID id) {
        return catalogService.dataset(id);
    }

    @GetMapping("/import-jobs")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<ClimateImportJobResponse> importJobs(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return importService.list(status, pageable);
    }

    @GetMapping("/import-jobs/{id}")
    @PreAuthorize("hasAuthority('climate:read')")
    public ClimateImportJobResponse importJob(@PathVariable UUID id) {
        return importService.get(id);
    }

    @PostMapping("/import-jobs")
    @PreAuthorize("hasAnyAuthority('climate:import','climate:write','climate:admin')")
    public ResponseEntity<ClimateImportJobResponse> startImport(
        @RequestBody CreateClimateImportJobRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.start(request, actor.id()));
    }

    @GetMapping("/quality-reports")
    @PreAuthorize("hasAuthority('climate:read')")
    public PageResponse<ClimateQualityReportResponse> quality(@PageableDefault(size = 20) Pageable pageable) {
        return catalogService.qualityReports(pageable);
    }

    @GetMapping("/map/stations")
    @PreAuthorize("hasAuthority('climate:read')")
    public Map<String, Object> mapStations() {
        return catalogService.mapStations();
    }

    @GetMapping("/map/footprints")
    @PreAuthorize("hasAuthority('climate:read')")
    public Map<String, Object> mapFootprints() {
        return catalogService.mapFootprints();
    }

    @GetMapping("/reports/{name}")
    @PreAuthorize("hasAnyAuthority('reports:climate','climate:read')")
    public Map<String, Object> report(@PathVariable String name) {
        return catalogService.report(name);
    }
}
