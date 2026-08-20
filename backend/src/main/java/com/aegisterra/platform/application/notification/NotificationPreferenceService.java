package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.domain.notification.NotificationChannel;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationPreferenceEntity;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationPreferenceRepository;
import com.aegisterra.platform.application.contracts.NotificationPreferenceRequest;
import com.aegisterra.platform.application.contracts.NotificationPreferenceResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> list(UUID userId) {
        return preferenceRepository.findByUserIdAndDeletedFalse(userId).stream()
            .map(p -> new NotificationPreferenceResponse(p.getChannel(), p.getEventType(), p.isEnabled()))
            .toList();
    }

    @Transactional
    public List<NotificationPreferenceResponse> replaceAll(UUID userId, List<NotificationPreferenceRequest> requests) {
        List<NotificationPreferenceResponse> saved = new ArrayList<>();
        for (NotificationPreferenceRequest req : requests) {
            String channel = req.channel().trim().toUpperCase();
            String eventType = req.eventType().trim();
            NotificationPreferenceEntity entity = preferenceRepository
                .findByUserIdAndChannelAndEventTypeAndDeletedFalse(userId, channel, eventType)
                .orElseGet(NotificationPreferenceEntity::new);
            if (entity.getId() == null) {
                entity.setCreatedBy(userId);
                entity.setDeleted(false);
            }
            entity.setUserId(userId);
            entity.setChannel(channel);
            entity.setEventType(eventType);
            entity.setEnabled(req.enabled());
            entity.setStatus("ACTIVE");
            entity.setUpdatedBy(userId);
            preferenceRepository.save(entity);
            saved.add(new NotificationPreferenceResponse(entity.getChannel(), entity.getEventType(), entity.isEnabled()));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId, NotificationChannel channel, String eventType) {
        return preferenceRepository
            .findByUserIdAndChannelAndEventTypeAndDeletedFalse(userId, channel.name(), eventType)
            .map(NotificationPreferenceEntity::isEnabled)
            .orElseGet(() -> channel == NotificationChannel.IN_APP);
    }
}
