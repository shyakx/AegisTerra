package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.identity.AuditService;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAuditHelper {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WorkflowAuditHelper(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public void record(AuditAction action, UUID actorId, String resourceType, UUID resourceId, Object details) {
        try {
            auditService.record(
                action,
                actorId,
                resourceType,
                resourceId != null ? resourceId.toString() : null,
                null,
                null,
                objectMapper.writeValueAsString(details == null ? Map.of() : details)
            );
        } catch (JsonProcessingException e) {
            auditService.record(action, actorId, resourceType,
                resourceId != null ? resourceId.toString() : null, null, null, "{\"error\":\"serialization_failed\"}");
        }
    }

    public Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
