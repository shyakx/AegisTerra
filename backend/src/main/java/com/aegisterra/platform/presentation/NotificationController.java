package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.notification.NotificationHistoryService;
import com.aegisterra.platform.application.notification.NotificationPreferenceService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.NotificationPreferenceRequest;
import com.aegisterra.platform.application.contracts.NotificationPreferenceResponse;
import com.aegisterra.platform.application.contracts.NotificationResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationHistoryService historyService;
    private final NotificationPreferenceService preferenceService;

    public NotificationController(
        NotificationHistoryService historyService,
        NotificationPreferenceService preferenceService
    ) {
        this.historyService = historyService;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('notifications:read')")
    @Operation(summary = "List my notifications")
    public PageResponse<NotificationResponse> list(
        @AuthenticationPrincipal AegisUserPrincipal actor,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return historyService.list(actor.id(), pageable(page, size));
    }

    @GetMapping("/notifications/unread")
    @PreAuthorize("hasAuthority('notifications:read')")
    @Operation(summary = "List unread notifications")
    public PageResponse<NotificationResponse> unread(
        @AuthenticationPrincipal AegisUserPrincipal actor,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return historyService.unread(actor.id(), pageable(page, size));
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasAuthority('notifications:read')")
    @Operation(summary = "Unread notification count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AegisUserPrincipal actor) {
        return Map.of("count", historyService.unreadCount(actor.id()));
    }

    @GetMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('notifications:read')")
    @Operation(summary = "Notification detail")
    public NotificationResponse get(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return historyService.get(actor.id(), id);
    }

    @PostMapping("/notifications/{id}/read")
    @PreAuthorize("hasAuthority('notifications:write')")
    @Operation(summary = "Mark notification as read")
    public NotificationResponse markRead(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return historyService.markRead(actor.id(), id);
    }

    @GetMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('notifications:read')")
    @Operation(summary = "Get my notification preferences")
    public List<NotificationPreferenceResponse> preferences(@AuthenticationPrincipal AegisUserPrincipal actor) {
        return preferenceService.list(actor.id());
    }

    @PutMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('notifications:write')")
    @Operation(summary = "Replace my notification preferences")
    public List<NotificationPreferenceResponse> updatePreferences(
        @AuthenticationPrincipal AegisUserPrincipal actor,
        @Valid @RequestBody List<NotificationPreferenceRequest> body
    ) {
        return preferenceService.replaceAll(actor.id(), body == null ? List.of() : body);
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
