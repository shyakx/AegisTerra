package com.aegisterra.platform.infrastructure.persistence.engagement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByOwnerTypeAndOwnerIdAndDeletedFalse(String ownerType, UUID ownerId);
}
