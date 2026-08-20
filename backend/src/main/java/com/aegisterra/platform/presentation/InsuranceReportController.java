package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.insurance.PolicyReportingService;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurance/reports")
@Tag(name = "Insurance Reports")
public class InsuranceReportController {

    private final PolicyReportingService reportingService;

    public InsuranceReportController(PolicyReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/active-policies")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse active() {
        return reportingService.activePolicies();
    }

    @GetMapping("/expired-policies")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse expired() {
        return reportingService.expiredPolicies();
    }

    @GetMapping("/coverage-by-crop")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse byCrop() {
        return reportingService.byCrop();
    }

    @GetMapping("/premium-revenue")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse revenue() {
        return reportingService.premiumRevenue();
    }

    @GetMapping("/portfolio-distribution")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse portfolio() {
        return reportingService.portfolioDistribution();
    }

    @GetMapping("/renewals")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse renewals() {
        return reportingService.renewals();
    }

    @GetMapping("/cancellations")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceReportResponse cancellations() {
        return reportingService.cancellations();
    }
}
