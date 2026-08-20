package com.aegisterra.platform.application.notification.channel;

import com.aegisterra.platform.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        log.info("[EMAIL stub] toUser={} title={} event={}", message.recipientUserId(), message.title(), message.eventType());
        return DeliveryResult.ok(null, "LoggingEmailProvider");
    }
}
