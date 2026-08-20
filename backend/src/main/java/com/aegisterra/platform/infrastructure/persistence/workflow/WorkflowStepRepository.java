package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStepEntity, UUID> {
    List<WorkflowStepEntity> findByInstanceIdAndDeletedFalseOrderByEnteredAtAsc(UUID instanceId);
    Optional<WorkflowStepEntity> findFirstByInstanceIdAndStatusAndDeletedFalseOrderByEnteredAtDesc(
        UUID instanceId, String status
    );
}
