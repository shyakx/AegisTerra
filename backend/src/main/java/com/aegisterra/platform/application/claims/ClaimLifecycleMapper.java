package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.claims.ClaimStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClaimLifecycleMapper {

    public Optional<ClaimStatus> mapWorkflowStep(String stepCode) {
        if (stepCode == null || stepCode.isBlank()) {
            return Optional.empty();
        }
        return switch (stepCode.trim().toUpperCase()) {
            case "VALIDATION" -> Optional.of(ClaimStatus.UNDER_VALIDATION);
            case "INSPECTION" -> Optional.of(ClaimStatus.INSPECTION);
            case "ASSESSMENT" -> Optional.of(ClaimStatus.ASSESSMENT);
            case "DECISION" -> Optional.of(ClaimStatus.PENDING_DECISION);
            default -> Optional.empty();
        };
    }

    public Optional<ClaimStatus> mapTerminalStep(String stepCode) {
        if (stepCode == null || stepCode.isBlank()) {
            return Optional.empty();
        }
        return switch (stepCode.trim().toUpperCase()) {
            case "DONE_APPROVED" -> Optional.of(ClaimStatus.APPROVED);
            case "DONE_REJECTED" -> Optional.of(ClaimStatus.REJECTED);
            case "DONE_RETURNED" -> Optional.of(ClaimStatus.RETURNED_FOR_INFO);
            default -> Optional.empty();
        };
    }
}
