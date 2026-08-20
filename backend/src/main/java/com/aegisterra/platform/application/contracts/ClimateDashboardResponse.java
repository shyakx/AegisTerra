package com.aegisterra.platform.application.contracts;

public record ClimateDashboardResponse(
    long providerCount,
    long enabledProviderCount,
    long stationCount,
    long observationCountRecent,
    long openImportJobs,
    String latestQualityGrade
) {}
