package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.workflow.WorkflowInstanceService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.WorkflowInstanceResponse;
import com.aegisterra.platform.application.contracts.WorkflowStartRequest;
import com.aegisterra.platform.application.contracts.WorkflowTransitionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/workflows/instances")
@Tag(name = "Workflow Instances")
public class WorkflowInstanceController {

    private final WorkflowInstanceService instanceService;

    public WorkflowInstanceController(WorkflowInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('workflows:write')")
    @Operation(summary = "Start a workflow instance against the published definition version")
    public ResponseEntity<WorkflowInstanceResponse> start(
        @Valid @RequestBody WorkflowStartRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanceService.start(request, actor.id()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflows:read')")
    @Operation(summary = "Load workflow instance with timeline events")
    public WorkflowInstanceResponse get(@PathVariable UUID id) {
        return instanceService.get(id);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAuthority('workflows:write')")
    @Operation(summary = "Execute a validated transition on a running instance")
    public WorkflowInstanceResponse transition(
        @PathVariable UUID id,
        @Valid @RequestBody WorkflowTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return instanceService.transition(id, request, actor.id());
    }
}
