package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.workflow.TaskQueryService;
import com.aegisterra.platform.application.workflow.TaskService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.TaskAssignRequest;
import com.aegisterra.platform.application.contracts.TaskCancelRequest;
import com.aegisterra.platform.application.contracts.TaskCommentRequest;
import com.aegisterra.platform.application.contracts.TaskCompleteRequest;
import com.aegisterra.platform.application.contracts.WorkflowTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Workflow Tasks")
public class WorkflowTaskController {

    private final TaskQueryService queryService;
    private final TaskService taskService;

    public WorkflowTaskController(TaskQueryService queryService, TaskService taskService) {
        this.queryService = queryService;
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tasks:read')")
    @Operation(summary = "Search task inbox")
    public PageResponse<WorkflowTaskResponse> search(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String subjectType,
        @RequestParam(required = false) String taskType,
        @RequestParam(required = false) UUID assigneeUserId,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return queryService.search(status, subjectType, taskType, assigneeUserId, roleCode, q, pageable(page, size, sort));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('tasks:read')")
    @Operation(summary = "My assigned and claimable role-pool tasks")
    public PageResponse<WorkflowTaskResponse> my(
        @AuthenticationPrincipal AegisUserPrincipal actor,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String subjectType,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return queryService.myInbox(actor.id(), status, subjectType, q, pageable(page, size, sort));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tasks:read')")
    @Operation(summary = "Task detail with comments, assignments, workflow timeline")
    public WorkflowTaskResponse get(@PathVariable UUID id) {
        return queryService.get(id);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('tasks:assign')")
    public WorkflowTaskResponse assign(
        @PathVariable UUID id,
        @RequestBody TaskAssignRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return taskService.assign(id, request, actor.id());
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('tasks:act')")
    public WorkflowTaskResponse claim(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return taskService.claim(id, actor.id());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('tasks:act')")
    public WorkflowTaskResponse complete(
        @PathVariable UUID id,
        @RequestBody(required = false) TaskCompleteRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return taskService.complete(id, request == null ? new TaskCompleteRequest(null, null, null) : request, actor.id());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('tasks:act','tasks:assign')")
    public WorkflowTaskResponse cancel(
        @PathVariable UUID id,
        @Valid @RequestBody TaskCancelRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return taskService.cancel(id, request, actor.id());
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('tasks:act')")
    public WorkflowTaskResponse comment(
        @PathVariable UUID id,
        @Valid @RequestBody TaskCommentRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return taskService.addComment(id, request, actor.id());
    }

    private static PageRequest pageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(dir, parts[0]));
    }
}
