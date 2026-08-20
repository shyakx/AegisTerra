package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationEntity;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationRepository;
import com.aegisterra.platform.application.contracts.NotificationResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationHistoryService {

    private final NotificationRepository notificationRepository;

    public NotificationHistoryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        return toPage(notificationRepository.findByUserIdAndDeletedFalse(userId, pageable));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> unread(UUID userId, Pageable pageable) {
        return toPage(notificationRepository.findUnreadByUserId(userId, pageable));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndDeletedFalseAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID userId, UUID id) {
        return toResponse(requireOwned(userId, id));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        NotificationEntity entity = requireOwned(userId, id);
        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
            entity.setUpdatedBy(userId);
            if ("DELIVERED".equals(entity.getStatus())) {
                entity.setStatus("READ");
            }
        }
        return toResponse(entity);
    }

    private NotificationEntity requireOwned(UUID userId, UUID id) {
        return notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private static PageResponse<NotificationResponse> toPage(Page<NotificationEntity> page) {
        return new PageResponse<>(
            page.getContent().stream().map(NotificationHistoryService::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    static NotificationResponse toResponse(NotificationEntity n) {
        return new NotificationResponse(
            n.getId(),
            n.getChannel(),
            n.getTitle(),
            n.getBody(),
            n.getEventType(),
            n.getSubjectType(),
            n.getSubjectId(),
            n.getCorrelationId(),
            n.getTemplateCode(),
            n.getSentAt(),
            n.getReadAt(),
            n.getStatus(),
            n.getCreatedAt()
        );
    }
}
