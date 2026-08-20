package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record InsuranceReportResponse(
    String reportCode,
    List<Map<String, Object>> rows,
    long totalCount,
    BigDecimal totalAmount
) {}
