package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentEntity, UUID> {
    List<TaskAssignmentEntity> findByTaskIdAndDeletedFalseOrderByOccurredAtAsc(UUID taskId);
}
