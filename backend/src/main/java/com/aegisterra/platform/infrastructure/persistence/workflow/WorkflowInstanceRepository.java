package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {
    Optional<WorkflowInstanceEntity> findByIdAndDeletedFalse(UUID id);
}
