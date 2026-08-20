package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.climateintelligence.ClimateIntelligenceService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
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
@RequestMapping("/api/v1/climate-intel")
@Tag(name = "Climate Intelligence")
public class ClimateIntelligenceController {

    private final ClimateIntelligenceService intelligenceService;

    public ClimateIntelligenceController(ClimateIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/national/dashboard")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public NationalRiskDashboardResponse nationalDashboard() {
        return intelligenceService.nationalDashboard();
    }

    @GetMapping("/farms/{farmId}/risk-score")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public FarmRiskScoreResponse farmRisk(@PathVariable UUID farmId) {
        return intelligenceService.farmRisk(farmId);
    }

    @GetMapping("/farms/{farmId}/profile")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public FarmClimateProfileResponse farmProfile(@PathVariable UUID farmId) {
        return intelligenceService.farmProfile(farmId);
    }

    @GetMapping("/farms/{farmId}/timeline")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public List<ClimateTimelineItemResponse> timeline(@PathVariable UUID farmId) {
        return intelligenceService.timeline(farmId);
    }

    @GetMapping("/farms/{farmId}/weather-summary")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public WeatherSummaryResponse weatherSummary(
        @PathVariable UUID farmId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return intelligenceService.weatherSummary(farmId, from, to);
    }

    @GetMapping("/farms/{farmId}/season-summary")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public SeasonSummaryResponse seasonSummary(
        @PathVariable UUID farmId,
        @RequestParam(required = false) String seasonCode
    ) {
        return intelligenceService.seasonSummary(farmId, seasonCode);
    }

    @GetMapping("/districts/{code}/risk-profile")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public DistrictRiskProfileResponse district(@PathVariable String code) {
        return intelligenceService.districtProfile(code);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyAuthority('climate-intel:read','alerts:climate')")
    public PageResponse<ClimateAlertResponse> alerts(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String alertType,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String scopeType,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return intelligenceService.alerts(status, alertType, severity, scopeType, pageable);
    }

    @GetMapping("/alerts/{id}")
    @PreAuthorize("hasAnyAuthority('climate-intel:read','alerts:climate')")
    public ClimateAlertResponse alert(@PathVariable UUID id) {
        return intelligenceService.alert(id);
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyAuthority('climate-intel:write','alerts:climate')")
    public ClimateAlertResponse acknowledge(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return intelligenceService.acknowledge(id, actor.id());
    }

    @PostMapping("/jobs/recalculate")
    @PreAuthorize("hasAnyAuthority('climate-intel:admin','climate-intel:write')")
    public ResponseEntity<ClimateIntelJobResponse> recalculate(
        @RequestBody(required = false) RecalculateClimateIntelRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        RecalculateClimateIntelRequest body = request == null
            ? new RecalculateClimateIntelRequest(null, null, null, null)
            : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(intelligenceService.recalculate(body, actor.id()));
    }

    @GetMapping("/indicators")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public PageResponse<ClimateIndicatorResponse> indicators(
        @RequestParam(required = false) String indicatorType,
        @RequestParam(required = false) String subjectType,
        @RequestParam(required = false) String subjectId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return intelligenceService.indicators(indicatorType, subjectType, subjectId, pageable);
    }

    @GetMapping("/map/risk")
    @PreAuthorize("hasAuthority('climate-intel:read')")
    public Map<String, Object> mapRisk() {
        return intelligenceService.mapRisk();
    }

    @GetMapping("/reports/{name}")
    @PreAuthorize("hasAnyAuthority('reports:climate-intel','climate-intel:read')")
    public Map<String, Object> report(@PathVariable String name) {
        return intelligenceService.report(name);
    }
}
