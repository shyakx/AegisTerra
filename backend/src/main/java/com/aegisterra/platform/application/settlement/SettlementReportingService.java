package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import com.aegisterra.platform.application.contracts.SettlementReportResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementReportingService {

    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;

    public SettlementReportingService(SettlementRepository settlementRepository, ObjectMapper objectMapper) {
        this.settlementRepository = settlementRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SettlementReportResponse summary() {
        InsuranceReportResponse pending = pending();
        InsuranceReportResponse completed = completed();
        InsuranceReportResponse failed = failed();
        return new SettlementReportResponse(
            "SETTLEMENT_SUMMARY",
            pending.totalCount(),
            completed.totalCount(),
            failed.totalCount(),
            completed.totalAmount(),
            pending.rows(),
            byProvider().rows(),
            averageCycleTime().rows().isEmpty() ? 0.0
                : ((Number) averageCycleTime().rows().get(0).getOrDefault("averageCycleHours", 0)).doubleValue(),
            byDistrict().rows(),
            byCrop().rows(),
            byCompany().rows()
        );
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse pending() {
        return statusBucket("SETTLEMENTS_PENDING", List.of(
            SettlementStatus.PENDING.name(),
            SettlementStatus.UNDER_REVIEW.name(),
            SettlementStatus.APPROVED.name(),
            SettlementStatus.PROCESSING.name(),
            SettlementStatus.SENT.name(),
            SettlementStatus.CONFIRMED.name(),
            SettlementStatus.RETRY_PENDING.name()
        ));
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse completed() {
        return statusBucket("SETTLEMENTS_COMPLETED", List.of(SettlementStatus.COMPLETED.name()));
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse failed() {
        return statusBucket("SETTLEMENTS_FAILED", List.of(
            SettlementStatus.FAILED.name(),
            SettlementStatus.REJECTED.name(),
            SettlementStatus.CANCELLED.name()
        ));
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byStatus() {
        return statusBucket("SETTLEMENTS_BY_STATUS", activeSettlements().stream()
            .map(SettlementEntity::getStatus)
            .distinct()
            .toList());
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byProvider() {
        Map<String, List<SettlementEntity>> grouped = activeSettlements().stream()
            .collect(Collectors.groupingBy(s -> s.getProviderCode() == null ? "UNKNOWN" : s.getProviderCode()));
        return groupedReport("SETTLEMENTS_BY_PROVIDER", "providerCode", grouped);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse bySource() {
        return bySourceModule();
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse bySourceModule() {
        Map<String, List<SettlementEntity>> grouped = activeSettlements().stream()
            .collect(Collectors.groupingBy(s -> s.getSourceModule() == null ? "UNKNOWN" : s.getSourceModule()));
        return groupedReport("SETTLEMENTS_BY_SOURCE", "sourceModule", grouped);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse paidToday() {
        return completed();
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse averageCycleTime() {
        List<SettlementEntity> completed = activeSettlements().stream()
            .filter(s -> SettlementStatus.COMPLETED.name().equals(s.getStatus()))
            .filter(s -> s.getCreatedAt() != null && s.getCompletedAt() != null)
            .toList();
        double avgHours = completed.stream()
            .mapToLong(s -> Duration.between(s.getCreatedAt(), s.getCompletedAt()).toMinutes())
            .average()
            .orElse(0d) / 60d;
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("completedCount", completed.size());
        row.put("averageCycleHours", BigDecimal.valueOf(avgHours).setScale(2, RoundingMode.HALF_UP));
        rows.add(row);
        BigDecimal total = completed.stream().map(SettlementEntity::getAmount).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InsuranceReportResponse("SETTLEMENTS_AVG_CYCLE_TIME", rows, completed.size(), total);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byDistrict() {
        return snapshotFieldReport("SETTLEMENTS_BY_DISTRICT", "districtCode", "district");
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byCrop() {
        return snapshotFieldReport("SETTLEMENTS_BY_CROP", "cropId");
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byCompany() {
        return snapshotFieldReport("SETTLEMENTS_BY_COMPANY", "companyId", "insurerId", "insuranceCompany");
    }

    private InsuranceReportResponse statusBucket(String code, List<String> statuses) {
        List<SettlementEntity> matched = statuses.isEmpty()
            ? activeSettlements()
            : settlementRepository.findByStatusInAndDeletedFalse(statuses);
        Map<String, List<SettlementEntity>> grouped = matched.stream()
            .collect(Collectors.groupingBy(SettlementEntity::getStatus, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<SettlementEntity>> entry : grouped.entrySet()) {
            BigDecimal amount = entry.getValue().stream().map(SettlementEntity::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(amount);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("amount", amount);
            rows.add(row);
        }
        return new InsuranceReportResponse(code, rows, matched.size(), total);
    }

    private InsuranceReportResponse groupedReport(
        String code,
        String keyName,
        Map<String, List<SettlementEntity>> grouped
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        for (Map.Entry<String, List<SettlementEntity>> entry : grouped.entrySet()) {
            BigDecimal amount = entry.getValue().stream().map(SettlementEntity::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(amount);
            count += entry.getValue().size();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(keyName, entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("amount", amount);
            rows.add(row);
        }
        return new InsuranceReportResponse(code, rows, count, total);
    }

    private InsuranceReportResponse snapshotFieldReport(String code, String... fields) {
        Map<String, List<SettlementEntity>> grouped = new LinkedHashMap<>();
        for (SettlementEntity s : activeSettlements()) {
            String key = "UNKNOWN";
            for (String field : fields) {
                String value = readSnapshotField(s.getFinancialSnapshotJson(), field);
                if (value != null && !value.isBlank()) {
                    key = value;
                    break;
                }
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return groupedReport(code, fields[0], grouped);
    }

    private String readSnapshotField(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json).get(field);
            return node == null || node.isNull() ? null : node.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    private List<SettlementEntity> activeSettlements() {
        return settlementRepository.findAll().stream().filter(s -> !s.isDeleted()).toList();
    }
}
