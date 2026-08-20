package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.application.identity.AuditService;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AgricultureAuditHelper {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AgricultureAuditHelper(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public void record(
        AuditAction action,
        UUID actorId,
        String resourceType,
        UUID resourceId,
        Object oldValue,
        Object newValue,
        String reason
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("oldValue", oldValue);
        details.put("newValue", newValue);
        if (reason != null && !reason.isBlank()) {
            details.put("reason", reason);
        }
        auditService.record(
            action,
            actorId,
            resourceType,
            resourceId != null ? resourceId.toString() : null,
            null,
            null,
            toJson(details)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}
