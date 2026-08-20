package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.PermissionView;
import com.aegisterra.platform.application.contracts.RoleView;
import com.aegisterra.platform.application.identity.RbacQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "RBAC")
public class RbacController {

    private final RbacQueryService rbacQueryService;

    public RbacController(RbacQueryService rbacQueryService) {
        this.rbacQueryService = rbacQueryService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('roles:read')")
    public ResponseEntity<List<RoleView>> listRoles() {
        return ResponseEntity.ok(rbacQueryService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permissions:read')")
    public ResponseEntity<List<PermissionView>> listPermissions() {
        return ResponseEntity.ok(rbacQueryService.listPermissions());
    }
}
