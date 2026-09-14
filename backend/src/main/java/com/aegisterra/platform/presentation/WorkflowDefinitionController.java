package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.workflow.WorkflowDefinitionService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionResponse;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionVersionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/workflows/definitions")
@Tag(name = "Workflow Definitions")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService definitionService;

    public WorkflowDefinitionController(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('workflows:read')")
    @Operation(summary = "List workflow definitions")
    public List<WorkflowDefinitionResponse> list() {
        return definitionService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflows:read')")
    @Operation(summary = "Get workflow definition with versions")
    public WorkflowDefinitionResponse get(@PathVariable UUID id) {
        return definitionService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('workflows:admin')")
    @Operation(summary = "Create workflow definition with draft version 1")
    public ResponseEntity<WorkflowDefinitionResponse> create(
        @Valid @RequestBody WorkflowDefinitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.create(request, actor.id()));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('workflows:admin')")
    @Operation(summary = "Create a new draft version (published graphs remain immutable)")
    public ResponseEntity<WorkflowDefinitionVersionResponse> createVersion(
        @PathVariable UUID id,
        @Valid @RequestBody WorkflowDefinitionVersionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.createVersion(id, request, actor.id()));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('workflows:admin')")
    @Operation(summary = "Publish the current draft version")
    public WorkflowDefinitionResponse publish(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return definitionService.publish(id, actor.id());
    }
}
