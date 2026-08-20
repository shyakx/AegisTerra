package com.aegisterra.platform.domain.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DecisionEffectTest {

    @Test
    void advancesWorkflowFlags() {
        assertTrue(DecisionEffect.COMPLETE_AND_ADVANCE.advancesWorkflow());
        assertTrue(DecisionEffect.REJECT_AND_ADVANCE.advancesWorkflow());
        assertFalse(DecisionEffect.COMPLETE_ONLY.advancesWorkflow());
        assertFalse(DecisionEffect.REASSIGN.advancesWorkflow());
        assertEquals(DecisionEffect.CANCEL_TASK, DecisionEffect.parse("cancel_task"));
    }
}
