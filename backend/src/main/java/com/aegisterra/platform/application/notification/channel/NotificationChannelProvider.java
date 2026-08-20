package com.aegisterra.platform.application.notification.channel;

import com.aegisterra.platform.domain.notification.NotificationChannel;
import java.util.UUID;

public interface NotificationChannelProvider {
    NotificationChannel channel();

    DeliveryResult send(NotificationMessage message);

    record DeliveryResult(boolean success, UUID notificationId, String providerName, String errorMessage) {
        public static DeliveryResult ok(UUID notificationId, String provider) {
            return new DeliveryResult(true, notificationId, provider, null);
        }

        public static DeliveryResult fail(String provider, String error) {
            return new DeliveryResult(false, null, provider, error);
        }
    }
}
