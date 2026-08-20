package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.claims.ClaimReportingService;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/claims/reports")
@Tag(name = "Claim Reports")
public class ClaimReportController {

    private final ClaimReportingService reportingService;

    public ClaimReportController(ClaimReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('claims:read')")
    public InsuranceReportResponse report(@PathVariable String name) {
        return switch (name.trim().toLowerCase()) {
            case "by-status" -> reportingService.byStatus();
            case "by-type" -> reportingService.byType();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
    }
}
