package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachmentEntity, UUID> {
    List<TaskAttachmentEntity> findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(UUID taskId);
}
