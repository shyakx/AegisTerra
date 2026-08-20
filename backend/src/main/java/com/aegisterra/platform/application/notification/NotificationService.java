package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.application.notification.channel.NotificationMessage;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.notification.NotificationChannel;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates notification creation from domain events. Business modules must not call channel providers.
 */
@Service
public class NotificationService {

    private final TemplateService templateService;
    private final ChannelResolver channelResolver;
    private final NotificationPreferenceService preferenceService;
    private final NotificationDispatcher dispatcher;
    private final UserRepository userRepository;

    public NotificationService(
        TemplateService templateService,
        ChannelResolver channelResolver,
        NotificationPreferenceService preferenceService,
        NotificationDispatcher dispatcher,
        UserRepository userRepository
    ) {
        this.templateService = templateService;
        this.channelResolver = channelResolver;
        this.preferenceService = preferenceService;
        this.dispatcher = dispatcher;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyFromEvent(PlatformDomainEvent event, Set<UUID> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        Map<String, String> vars = toVars(event);
        for (UUID userId : recipientUserIds) {
            if (userId == null) {
                continue;
            }
            for (NotificationChannel channel : channelResolver.resolveChannels(event.eventType())) {
                if (!preferenceService.isEnabled(userId, channel, event.eventType())) {
                    continue;
                }
                TemplateService.RenderedTemplate rendered = templateService.render(
                    event.eventType(),
                    channel.name(),
                    vars
                );
                NotificationMessage message = new NotificationMessage(
                    userId,
                    rendered.title(),
                    rendered.body(),
                    event.eventType(),
                    event.eventId(),
                    event.subjectType(),
                    event.subjectId(),
                    event.correlationId(),
                    rendered.templateCode(),
                    event.payload()
                );
                dispatcher.dispatch(message, channel);
            }
        }
    }

    public Set<UUID> resolveRecipients(PlatformDomainEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();
        Object assignee = event.payload().get("assigneeUserId");
        if (assignee != null && !assignee.toString().isBlank()) {
            recipients.add(UUID.fromString(assignee.toString()));
        }
        Object roleCode = event.payload().get("assigneeRoleCode");
        if (roleCode != null && !roleCode.toString().isBlank()) {
            recipients.addAll(userRepository.findActiveUserIdsByRoleCode(roleCode.toString()));
        }
        if (recipients.isEmpty() && event.actorId() != null) {
            recipients.add(event.actorId());
        }
        return recipients;
    }

    private static Map<String, String> toVars(PlatformDomainEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("eventType", event.eventType());
        vars.put("subjectType", event.subjectType());
        vars.put("subjectId", event.subjectId() == null ? "" : event.subjectId().toString());
        vars.put("correlationId", event.correlationId() == null ? "" : event.correlationId());
        vars.put("actorId", event.actorId() == null ? "" : event.actorId().toString());
        event.payload().forEach((k, v) -> vars.put(k, v == null ? "" : String.valueOf(v)));
        return vars;
    }
}
