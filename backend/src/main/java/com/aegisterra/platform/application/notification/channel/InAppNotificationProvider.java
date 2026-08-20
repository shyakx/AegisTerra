package com.aegisterra.platform.application.notification.channel;

import com.aegisterra.platform.domain.notification.NotificationChannel;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationEntity;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationProvider implements InAppProvider {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public InAppNotificationProvider(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(message.recipientUserId());
        entity.setChannel(NotificationChannel.IN_APP.name());
        entity.setTitle(message.title());
        entity.setBody(message.body());
        entity.setSentAt(Instant.now());
        entity.setEventId(message.eventId());
        entity.setEventType(message.eventType());
        entity.setSubjectType(message.subjectType());
        entity.setSubjectId(message.subjectId());
        entity.setCorrelationId(message.correlationId());
        entity.setTemplateCode(message.templateCode());
        try {
            entity.setPayloadJson(objectMapper.writeValueAsString(message.payload()));
        } catch (Exception ex) {
            entity.setPayloadJson("{}");
        }
        entity.setStatus("DELIVERED");
        entity.setDeleted(false);
        entity.setCreatedBy(message.recipientUserId());
        notificationRepository.save(entity);
        return DeliveryResult.ok(entity.getId(), "InAppProvider");
    }
}
