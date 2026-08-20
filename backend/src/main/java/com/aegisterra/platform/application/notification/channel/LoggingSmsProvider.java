package com.aegisterra.platform.application.notification.channel;

import com.aegisterra.platform.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        log.info("[SMS stub] toUser={} title={} event={}", message.recipientUserId(), message.title(), message.eventType());
        return DeliveryResult.ok(null, "LoggingSmsProvider");
    }
}
