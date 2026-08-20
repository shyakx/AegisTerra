package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.application.contracts.InsuranceReportResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimReportingService {

    private final ClaimRepository claimRepository;

    public ClaimReportingService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byStatus() {
        List<ClaimEntity> claims = claimRepository.findAll().stream().filter(c -> !c.isDeleted()).toList();
        Map<String, List<ClaimEntity>> grouped = claims.stream()
            .collect(Collectors.groupingBy(ClaimEntity::getStatus));
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<ClaimEntity>> entry : grouped.entrySet()) {
            BigDecimal amount = entry.getValue().stream()
                .map(ClaimEntity::getClaimedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(amount);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("claimedAmount", amount);
            rows.add(row);
        }
        return new InsuranceReportResponse("CLAIMS_BY_STATUS", rows, claims.size(), total);
    }

    @Transactional(readOnly = true)
    public InsuranceReportResponse byType() {
        List<ClaimEntity> claims = claimRepository.findAll().stream().filter(c -> !c.isDeleted()).toList();
        Map<String, List<ClaimEntity>> grouped = claims.stream()
            .collect(Collectors.groupingBy(c -> c.getClaimTypeCode() == null ? "UNKNOWN" : c.getClaimTypeCode()));
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<ClaimEntity>> entry : grouped.entrySet()) {
            BigDecimal amount = entry.getValue().stream()
                .map(ClaimEntity::getClaimedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(amount);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("claimTypeCode", entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("claimedAmount", amount);
            rows.add(row);
        }
        return new InsuranceReportResponse("CLAIMS_BY_TYPE", rows, claims.size(), total);
    }
}
