package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExecutiveOverviewResponse(
    Instant generatedAt,
    CountBlock farmers,
    CountBlock farms,
    CountBlock policies,
    CountBlock claims,
    SettlementBlock settlements,
    ClimateBlock climate,
    TasksBlock tasks,
    NotificationsBlock notifications,
    List<RegionalRow> regional,
    List<QuickLink> quickLinks
) {
    public record CountBlock(
        long total,
        Long active,
        Long open,
        Long draft,
        Long approved,
        Long rejected,
        Long withBoundary
    ) {
        public static CountBlock of(long total) {
            return new CountBlock(total, null, null, null, null, null, null);
        }
    }

    public record SettlementBlock(
        long total,
        long pending,
        long completed,
        long failed,
        Double pendingAmount,
        Double completedAmount,
        String currency
    ) {}

    public record ClimateBlock(
        long stations,
        long recentObservations,
        long openAlerts,
        long criticalAlerts,
        Map<String, Long> farmsByRiskGrade
    ) {}

    public record TasksBlock(long pending) {}

    public record NotificationsBlock(long unread) {}

    public record RegionalRow(String districtCode, Double meanScore, String grade, long farmCount) {}

    public record QuickLink(String label, String path, String permission) {}
}
