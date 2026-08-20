package com.aegisterra.platform.infrastructure.persistence.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    Page<NotificationEntity> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    @Query("""
        select n from NotificationEntity n
        where n.userId = :userId and n.deleted = false and n.readAt is null
        order by n.createdAt desc
        """)
    Page<NotificationEntity> findUnreadByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndDeletedFalseAndReadAtIsNull(UUID userId);

    Optional<NotificationEntity> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
