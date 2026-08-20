package com.aegisterra.platform.infrastructure.persistence.engagement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {
    Optional<NotificationTemplateEntity> findByCodeAndChannelAndLocaleAndDeletedFalse(
        String code,
        String channel,
        String locale
    );
}
