package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.settlement.SettlementReportingService;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import com.aegisterra.platform.application.contracts.SettlementReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/settlements/reports")
@Tag(name = "Settlement Reports")
public class SettlementReportController {

    private final SettlementReportingService reportingService;

    public SettlementReportController(SettlementReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('reports:settlements') or hasAuthority('settlements:read')")
    @Operation(summary = "Settlement reporting dashboard summary")
    public SettlementReportResponse summary() {
        return reportingService.summary();
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('reports:settlements') or hasAuthority('settlements:read')")
    @Operation(summary = "Named settlement report")
    public InsuranceReportResponse report(@PathVariable String name) {
        return switch (name.trim().toLowerCase()) {
            case "pending" -> reportingService.pending();
            case "completed" -> reportingService.completed();
            case "failed" -> reportingService.failed();
            case "by-provider" -> reportingService.byProvider();
            case "by-source", "by-source-module" -> reportingService.bySource();
            case "by-status" -> reportingService.byStatus();
            case "paid-today" -> reportingService.paidToday();
            case "avg-cycle-time", "average-cycle-time", "avg-cycle" -> reportingService.averageCycleTime();
            case "by-district" -> reportingService.byDistrict();
            case "by-crop" -> reportingService.byCrop();
            case "by-company" -> reportingService.byCompany();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
    }
}
