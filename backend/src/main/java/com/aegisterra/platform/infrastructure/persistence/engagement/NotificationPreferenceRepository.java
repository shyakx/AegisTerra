package com.aegisterra.platform.infrastructure.persistence.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {
    List<NotificationPreferenceEntity> findByUserIdAndDeletedFalse(UUID userId);

    Optional<NotificationPreferenceEntity> findByUserIdAndChannelAndEventTypeAndDeletedFalse(
        UUID userId,
        String channel,
        String eventType
    );
}
