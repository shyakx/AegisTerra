package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SettlementReportResponse(
    String reportCode,
    long pendingCount,
    long completedCount,
    long failedCount,
    BigDecimal completedTotal,
    List<Map<String, Object>> byStatus,
    List<Map<String, Object>> byProvider,
    Double averageSettlementHours,
    List<Map<String, Object>> byDistrict,
    List<Map<String, Object>> byCrop,
    List<Map<String, Object>> byInsuranceCompany
) {}
