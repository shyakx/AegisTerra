package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.application.contracts.PermissionView;
import com.aegisterra.platform.application.contracts.RoleView;
import com.aegisterra.platform.infrastructure.persistence.identity.PermissionRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacQueryService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacQueryService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return roleRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .map(role -> new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystemRole()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions() {
        return permissionRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .map(permission -> new PermissionView(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getResource(),
                permission.getAction()
            ))
            .toList();
    }
}
