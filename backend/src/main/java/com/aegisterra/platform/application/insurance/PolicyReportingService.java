package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyReportingService {

    private final InsurancePolicyRepository policyRepository;

    public PolicyReportingService(InsurancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse activePolicies() {
        return byStatus("ACTIVE");
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse expiredPolicies() {
        return byStatus("EXPIRED");
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse cancellations() {
        return byStatus("CANCELLED");
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byCrop() {
        List<InsurancePolicyEntity> policies = policyRepository.findAll().stream()
            .filter(p -> !p.isDeleted())
            .toList();
        Map<String, List<InsurancePolicyEntity>> grouped = policies.stream()
            .collect(Collectors.groupingBy(p -> p.getCropId() == null ? "UNKNOWN" : p.getCropId().toString()));
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<InsurancePolicyEntity>> entry : grouped.entrySet()) {
            BigDecimal coverage = entry.getValue().stream()
                .map(InsurancePolicyEntity::getCoverageAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(coverage);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cropId", entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("coverageAmount", coverage);
            rows.add(row);
        }
        return new InsuranceReportResponse("COVERAGE_BY_CROP", rows, policies.size(), total);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse premiumRevenue() {
        List<InsurancePolicyEntity> paidLike = policyRepository.findAll().stream()
            .filter(p -> !p.isDeleted())
            .filter(p -> "ACTIVE".equals(p.getStatus()) || "EXPIRED".equals(p.getStatus()))
            .toList();
        BigDecimal total = paidLike.stream().map(InsurancePolicyEntity::getPremiumAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("statusGroup", "ACTIVE_OR_EXPIRED");
        row.put("count", paidLike.size());
        row.put("premiumAmount", total);
        rows.add(row);
        return new InsuranceReportResponse("PREMIUM_REVENUE", rows, paidLike.size(), total);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse portfolioDistribution() {
        List<InsurancePolicyEntity> policies = policyRepository.findAll().stream().filter(p -> !p.isDeleted()).toList();
        Map<String, Long> byStatus = policies.stream()
            .collect(Collectors.groupingBy(InsurancePolicyEntity::getStatus, Collectors.counting()));
        List<Map<String, Object>> rows = byStatus.entrySet().stream().map(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", e.getKey());
            row.put("count", e.getValue());
            return row;
        }).toList();
        return new InsuranceReportResponse("PORTFOLIO_DISTRIBUTION", rows, policies.size(), null);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse renewals() {
        List<InsurancePolicyEntity> renewed = policyRepository.findAll().stream()
            .filter(p -> !p.isDeleted() && p.getParentPolicyId() != null)
            .toList();
        List<Map<String, Object>> rows = renewed.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("policyId", p.getId());
            row.put("policyNumber", p.getPolicyNumber());
            row.put("parentPolicyId", p.getParentPolicyId());
            row.put("status", p.getStatus());
            return row;
        }).toList();
        return new InsuranceReportResponse("RENEWALS", rows, renewed.size(), null);
    }

    private InsuranceReportResponse byStatus(String status) {
        List<InsurancePolicyEntity> policies = policyRepository.findAll().stream()
            .filter(p -> !p.isDeleted() && status.equals(p.getStatus()))
            .toList();
        BigDecimal total = policies.stream().map(InsurancePolicyEntity::getCoverageAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> rows = policies.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("policyNumber", p.getPolicyNumber());
            row.put("farmerId", p.getFarmerId());
            row.put("farmId", p.getFarmId());
            row.put("coverageAmount", p.getCoverageAmount());
            row.put("premiumAmount", p.getPremiumAmount());
            return row;
        }).toList();
        return new InsuranceReportResponse(status + "_POLICIES", rows, policies.size(), total);
    }
}
