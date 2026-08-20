package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, UUID> {
    List<WorkflowTransitionEntity> findByInstanceIdAndDeletedFalseOrderByOccurredAtAsc(UUID instanceId);
}
