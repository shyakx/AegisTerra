package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.AuditLogResponse;
import com.aegisterra.platform.application.contracts.CreateUserRequest;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.UpdateUserRequest;
import com.aegisterra.platform.application.contracts.UserResponse;
import com.aegisterra.platform.application.identity.UserAdminService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User administration (RBAC)")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Search users")
    public PageResponse<UserResponse> listUsers(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "username") Pageable pageable
    ) {
        return userAdminService.searchUsers(q, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Get user")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.getUser(id));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "User audit history")
    public ResponseEntity<List<AuditLogResponse>> listUserAudit(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.listUserAudit(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Create user")
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.createUser(request, actorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Update user")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        return ResponseEntity.ok(userAdminService.updateUser(id, request, actorId));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Disable user")
    public ResponseEntity<UserResponse> disableUser(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        return ResponseEntity.ok(userAdminService.disableUser(id, actorId));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Enable user")
    public ResponseEntity<UserResponse> enableUser(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        return ResponseEntity.ok(userAdminService.enableUser(id, actorId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID actorId = actor != null ? actor.id() : null;
        userAdminService.deleteUser(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
