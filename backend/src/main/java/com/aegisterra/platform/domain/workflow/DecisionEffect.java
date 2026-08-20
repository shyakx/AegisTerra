package com.aegisterra.platform.domain.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Effect applied when a decision is recorded. Stored on rules and decision history — not business-domain specific.
 */
public enum DecisionEffect {
    COMPLETE_AND_ADVANCE,
    COMPLETE_ONLY,
    REJECT_AND_ADVANCE,
    CANCEL_TASK,
    REASSIGN,
    KEEP_OPEN;

    public static DecisionEffect parse(String raw) {
        try {
            return DecisionEffect.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown decision effect: " + raw);
        }
    }

    public boolean advancesWorkflow() {
        return this == COMPLETE_AND_ADVANCE || this == REJECT_AND_ADVANCE;
    }
}
