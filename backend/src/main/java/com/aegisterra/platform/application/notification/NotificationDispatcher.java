package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.application.notification.channel.NotificationChannelProvider;
import com.aegisterra.platform.application.notification.channel.NotificationMessage;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.notification.NotificationChannel;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationDeliveryLogEntity;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationDeliveryLogRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDispatcher {

    private final ChannelResolver channelResolver;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final EventBus eventBus;

    public NotificationDispatcher(
        ChannelResolver channelResolver,
        NotificationDeliveryLogRepository deliveryLogRepository,
        EventBus eventBus
    ) {
        this.channelResolver = channelResolver;
        this.deliveryLogRepository = deliveryLogRepository;
        this.eventBus = eventBus;
    }

    @Transactional
    public void dispatch(NotificationMessage message, NotificationChannel channel) {
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.NOTIFICATION_REQUESTED,
            message.recipientUserId(),
            message.subjectType(),
            message.subjectId(),
            message.correlationId(),
            Map.of(
                "channel", channel.name(),
                "eventType", message.eventType(),
                "sourceEventId", message.eventId() == null ? "" : message.eventId().toString()
            )
        ));

        NotificationChannelProvider.DeliveryResult result;
        try {
            result = channelResolver.require(channel).send(message);
        } catch (Exception ex) {
            result = NotificationChannelProvider.DeliveryResult.fail(channel.name(), ex.getMessage());
        }

        logAttempt(message, channel, result);

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel.name());
        payload.put("sourceEventId", message.eventId() == null ? "" : message.eventId().toString());
        if (result.notificationId() != null) {
            payload.put("notificationId", result.notificationId().toString());
        }
        if (result.errorMessage() != null) {
            payload.put("error", result.errorMessage());
        }

        eventBus.publish(PlatformDomainEvent.of(
            result.success() ? DomainEventTypes.NOTIFICATION_DELIVERED : DomainEventTypes.NOTIFICATION_FAILED,
            message.recipientUserId(),
            message.subjectType(),
            message.subjectId(),
            message.correlationId(),
            payload
        ));
    }

    private void logAttempt(
        NotificationMessage message,
        NotificationChannel channel,
        NotificationChannelProvider.DeliveryResult result
    ) {
        NotificationDeliveryLogEntity log = new NotificationDeliveryLogEntity();
        log.setNotificationId(result.notificationId());
        log.setEventId(message.eventId());
        log.setChannel(channel.name());
        log.setAttemptNo(1);
        log.setDeliveryStatus(result.success() ? "DELIVERED" : "FAILED");
        log.setProvider(result.providerName());
        log.setErrorMessage(result.errorMessage());
        log.setAttemptedAt(Instant.now());
        log.setStatus("ACTIVE");
        log.setDeleted(false);
        log.setCreatedBy(message.recipientUserId());
        deliveryLogRepository.save(log);
    }
}
