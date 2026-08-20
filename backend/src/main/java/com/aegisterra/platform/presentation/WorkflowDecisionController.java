package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.workflow.DecisionHistoryService;
import com.aegisterra.platform.application.workflow.DecisionService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.DecisionTypeResponse;
import com.aegisterra.platform.application.contracts.TaskDecisionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDecisionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Workflow Decisions")
public class WorkflowDecisionController {

    private final DecisionService decisionService;
    private final DecisionHistoryService historyService;

    public WorkflowDecisionController(DecisionService decisionService, DecisionHistoryService historyService) {
        this.decisionService = decisionService;
        this.historyService = historyService;
    }

    @GetMapping("/decisions/types")
    @PreAuthorize("hasAnyAuthority('decisions:read','tasks:read')")
    @Operation(summary = "List active decision types")
    public List<DecisionTypeResponse> types() {
        return decisionService.listTypes();
    }

    @PostMapping("/tasks/{id}/decisions")
    @PreAuthorize("hasAnyAuthority('decisions:act','tasks:act')")
    @Operation(summary = "Record a decision on a task (validate, persist, route)")
    public WorkflowDecisionResponse decide(
        @PathVariable UUID id,
        @Valid @RequestBody TaskDecisionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return decisionService.decide(id, request, actor.id());
    }

    @GetMapping("/tasks/{id}/decisions")
    @PreAuthorize("hasAnyAuthority('decisions:read','tasks:read')")
    @Operation(summary = "Decision history for a task")
    public List<WorkflowDecisionResponse> history(@PathVariable UUID id) {
        return historyService.forTask(id);
    }
}
