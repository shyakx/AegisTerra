package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.domain.workflow.DecisionEffect;
import com.aegisterra.platform.infrastructure.persistence.workflow.DecisionRuleEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.DecisionRuleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DecisionRuleResolver {

    private final DecisionRuleRepository ruleRepository;

    public DecisionRuleResolver(DecisionRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public ResolvedRule resolve(String decisionTypeCode, String definitionCode, String stepCode, String taskType) {
        List<DecisionRuleEntity> candidates = ruleRepository
            .findByDecisionTypeCodeAndDeletedFalseAndStatusOrderByPriorityDesc(decisionTypeCode, "ACTIVE");
        DecisionRuleEntity best = candidates.stream()
            .filter(r -> matches(r.getWorkflowDefinitionCode(), definitionCode))
            .filter(r -> matches(r.getStepCode(), stepCode))
            .filter(r -> matches(r.getTaskType(), taskType))
            .max(Comparator
                .comparingInt(DecisionRuleEntity::getPriority)
                .thenComparingInt(r -> specificity(r)))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No decision rule for type " + decisionTypeCode));

        DecisionEffect effect = DecisionEffect.parse(best.getEffect());
        String action = best.getWorkflowAction() == null || best.getWorkflowAction().isBlank()
            ? null
            : best.getWorkflowAction().trim().toUpperCase();
        if (effect.advancesWorkflow() && (action == null || action.isBlank())) {
            action = decisionTypeCode;
        }
        return new ResolvedRule(best.getId(), best.getDecisionTypeCode(), best.getOutcomeCode(), effect, action);
    }

    private static boolean matches(String ruleValue, String actual) {
        return ruleValue == null || ruleValue.isBlank() || Objects.equals(ruleValue, actual);
    }

    private static int specificity(DecisionRuleEntity rule) {
        int score = 0;
        if (rule.getWorkflowDefinitionCode() != null && !rule.getWorkflowDefinitionCode().isBlank()) {
            score += 4;
        }
        if (rule.getStepCode() != null && !rule.getStepCode().isBlank()) {
            score += 2;
        }
        if (rule.getTaskType() != null && !rule.getTaskType().isBlank()) {
            score += 1;
        }
        return score;
    }

    public record ResolvedRule(
        java.util.UUID ruleId,
        String decisionTypeCode,
        String outcomeCode,
        DecisionEffect effect,
        String workflowAction
    ) {}
}
