package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.domain.settlement.SettlementStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SettlementLifecycleMapper {

    public Optional<SettlementStatus> mapWorkflowStep(String stepCode) {
        if (stepCode == null || stepCode.isBlank()) {
            return Optional.empty();
        }
        return switch (stepCode.trim().toUpperCase()) {
            case "FINANCE_REVIEW" -> Optional.of(SettlementStatus.UNDER_REVIEW);
            case "APPROVAL" -> Optional.of(SettlementStatus.APPROVED);
            case "DISBURSE" -> Optional.of(SettlementStatus.PROCESSING);
            case "CONFIRM" -> Optional.of(SettlementStatus.SENT);
            default -> Optional.empty();
        };
    }

    public Optional<SettlementStatus> mapTerminalStep(String stepCode) {
        if (stepCode == null || stepCode.isBlank()) {
            return Optional.empty();
        }
        return switch (stepCode.trim().toUpperCase()) {
            case "DONE_COMPLETED" -> Optional.of(SettlementStatus.COMPLETED);
            case "DONE_REJECTED" -> Optional.of(SettlementStatus.REJECTED);
            case "DONE_FAILED" -> Optional.of(SettlementStatus.FAILED);
            case "DONE_CANCELLED" -> Optional.of(SettlementStatus.CANCELLED);
            default -> Optional.empty();
        };
    }
}
