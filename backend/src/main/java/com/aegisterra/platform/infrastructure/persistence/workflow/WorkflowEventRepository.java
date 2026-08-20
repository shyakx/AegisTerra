package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEventRepository extends JpaRepository<WorkflowEventEntity, UUID> {
    List<WorkflowEventEntity> findByInstanceIdAndDeletedFalseOrderByOccurredAtAsc(UUID instanceId);
}
