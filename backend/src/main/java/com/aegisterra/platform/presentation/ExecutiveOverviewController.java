package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.executive.ExecutiveOverviewService;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse;
import com.aegisterra.platform.application.contracts.ExecutiveOverviewResponse.RegionalRow;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/executive")
@Tag(name = "Executive Overview")
public class ExecutiveOverviewController {

    private final ExecutiveOverviewService executiveOverviewService;

    public ExecutiveOverviewController(ExecutiveOverviewService executiveOverviewService) {
        this.executiveOverviewService = executiveOverviewService;
    }

    @GetMapping("/overview")
    @PreAuthorize(
        "hasAnyAuthority('farmers:read','policies:read','claims:read','settlements:read','climate-intel:read','climate:read')"
    )
    public ExecutiveOverviewResponse overview() {
        return executiveOverviewService.overview();
    }

    @GetMapping("/regional-summary")
    @PreAuthorize(
        "hasAnyAuthority('farmers:read','policies:read','claims:read','settlements:read','climate-intel:read','climate:read')"
    )
    public Map<String, List<RegionalRow>> regionalSummary() {
        return Map.of("districts", executiveOverviewService.regionalSummary());
    }
}
