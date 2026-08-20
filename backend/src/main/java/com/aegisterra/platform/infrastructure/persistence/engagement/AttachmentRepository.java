package com.aegisterra.platform.infrastructure.persistence.engagement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, UUID> {
    List<AttachmentEntity> findByDocumentIdAndDeletedFalse(UUID documentId);
}
