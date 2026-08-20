package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionRepository;
import com.aegisterra.platform.application.contracts.WorkflowDecisionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionHistoryService {

    private final WorkflowDecisionRepository decisionRepository;

    public DecisionHistoryService(WorkflowDecisionRepository decisionRepository) {
        this.decisionRepository = decisionRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDecisionResponse> forTask(UUID taskId) {
        return decisionRepository.findByTaskIdAndDeletedFalseOrderByDecidedAtAsc(taskId).stream()
            .map(this::toResponse)
            .toList();
    }

    public WorkflowDecisionResponse toResponse(WorkflowDecisionEntity entity) {
        return new WorkflowDecisionResponse(
            entity.getId(),
            entity.getTaskId(),
            entity.getInstanceId(),
            entity.getStepCode(),
            entity.getDecisionTypeCode(),
            entity.getOutcomeCode(),
            entity.getEffect(),
            entity.getWorkflowAction(),
            entity.getCommentText(),
            entity.getTargetUserId(),
            entity.getSubjectType(),
            entity.getSubjectId(),
            entity.getDecidedBy(),
            entity.getDecidedAt(),
            entity.getStatus()
        );
    }
}
