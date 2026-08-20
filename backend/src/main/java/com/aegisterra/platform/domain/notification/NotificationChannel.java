package com.aegisterra.platform.domain.notification;

public enum NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    PUSH,
    WEBHOOK;

    public static NotificationChannel parse(String raw) {
        return NotificationChannel.valueOf(raw.trim().toUpperCase());
    }
}
