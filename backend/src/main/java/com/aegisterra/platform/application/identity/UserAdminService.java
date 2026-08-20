package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.application.contracts.AuditLogResponse;
import com.aegisterra.platform.application.contracts.CreateUserRequest;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.UpdateUserRequest;
import com.aegisterra.platform.application.contracts.UserResponse;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.identity.PasswordPolicy;
import com.aegisterra.platform.infrastructure.persistence.identity.AuditLogEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.AuditLogRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.UserEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityAuditHelper auditHelper;

    public UserAdminService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        AuditLogRepository auditLogRepository,
        PasswordEncoder passwordEncoder,
        IdentityAuditHelper auditHelper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String q, String status, Pageable pageable) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String statusFilter = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        List<UserResponse> all = userRepository.findByDeletedFalseOrderByUsernameAsc().stream()
            .map(this::toResponse)
            .filter(u -> statusFilter == null || statusFilter.equalsIgnoreCase(u.status()))
            .filter(u -> query.isEmpty()
                || u.username().toLowerCase(Locale.ROOT).contains(query)
                || u.email().toLowerCase(Locale.ROOT).contains(query)
                || (u.displayName() != null && u.displayName().toLowerCase(Locale.ROOT).contains(query))
                || u.roles().stream().anyMatch(r -> r.toLowerCase(Locale.ROOT).contains(query)))
            .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<UserResponse> slice = start >= all.size() ? List.of() : all.subList(start, end);
        Page<UserResponse> page = new PageImpl<>(slice, pageable, all.size());
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findByDeletedFalseOrderByUsernameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return toResponse(requireUser(id));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listUserAudit(UUID id) {
        requireUser(id);
        return auditLogRepository
            .findTop50ByResourceTypeAndResourceIdOrderByCreatedAtDesc("user", id.toString())
            .stream()
            .map(this::toAudit)
            .toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID actorId) {
        if (userRepository.existsByUsernameIgnoreCaseAndDeletedFalse(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        List<String> errors = PasswordPolicy.validate(request.password(), request.username(), request.email());
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }

        UserEntity user = UserEntity.createLocal(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.displayName()
        );
        userRepository.save(user);

        List<String> roleCodes = request.roles() == null || request.roles().isEmpty()
            ? List.of("SUPPORT")
            : request.roles();
        assignRoles(user.getId(), roleCodes, actorId);
        UserResponse created = toResponse(user);
        auditHelper.record(AuditAction.USER_CREATED, actorId, "user", user.getId(), null, created, null);
        return created;
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, UUID actorId) {
        UserEntity user = requireUser(id);
        UserResponse old = toResponse(user);
        boolean statusChanged = false;
        if (request.email() != null && !request.email().isBlank()) {
            userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });
            user.setEmail(request.email());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.status() != null && !request.status().isBlank()) {
            statusChanged = !request.status().equalsIgnoreCase(user.getStatus());
            user.setStatus(request.status());
            user.bumpTokenVersion();
        }
        if (request.roles() != null) {
            for (UserRoleEntity existing : userRoleRepository.findByUserIdAndDeletedFalse(id)) {
                existing.setDeleted(true);
                existing.setStatus("REVOKED");
                userRoleRepository.save(existing);
                auditHelper.record(AuditAction.ROLE_REVOKED, actorId, "user", id, existing.getRoleId(), null, null);
            }
            assignRoles(id, request.roles(), actorId);
            user.bumpTokenVersion();
        }
        user.setUpdatedBy(actorId);
        userRepository.save(user);
        UserResponse updated = toResponse(user);
        if (statusChanged) {
            auditHelper.record(AuditAction.USER_STATUS_CHANGED, actorId, "user", id, old, updated, request.status());
        } else {
            auditHelper.record(AuditAction.USER_UPDATED, actorId, "user", id, old, updated, null);
        }
        return updated;
    }

    @Transactional
    public UserResponse disableUser(UUID id, UUID actorId) {
        return updateUser(id, new UpdateUserRequest(null, null, "DISABLED", null), actorId);
    }

    @Transactional
    public UserResponse enableUser(UUID id, UUID actorId) {
        return updateUser(id, new UpdateUserRequest(null, null, "ACTIVE", null), actorId);
    }

    @Transactional
    public void deleteUser(UUID id, UUID actorId) {
        UserEntity user = requireUser(id);
        UserResponse old = toResponse(user);
        user.setDeleted(true);
        user.setStatus("DELETED");
        user.bumpTokenVersion();
        user.setUpdatedBy(actorId);
        userRepository.save(user);
        auditHelper.record(AuditAction.USER_STATUS_CHANGED, actorId, "user", id, old, toResponse(user), "soft-delete");
    }

    private void assignRoles(UUID userId, List<String> roleCodes, UUID actorId) {
        for (String code : roleCodes) {
            RoleEntity role = roleRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + code));
            if (!userRoleRepository.existsByUserIdAndRoleIdAndDeletedFalse(userId, role.getId())) {
                userRoleRepository.save(UserRoleEntity.assign(userId, role.getId(), actorId));
                auditHelper.record(AuditAction.ROLE_ASSIGNED, actorId, "user", userId, null, code, null);
            }
        }
    }

    private UserEntity requireUser(UUID id) {
        return userRepository.findById(id)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserResponse toResponse(UserEntity user) {
        List<String> roles = new ArrayList<>();
        for (UserRoleEntity ur : userRoleRepository.findByUserIdAndDeletedFalse(user.getId())) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                if (!role.isDeleted()) {
                    roles.add(role.getCode());
                }
            });
        }
        String primary = roles.isEmpty() ? "NONE" : roles.get(0);
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            primary,
            roles,
            user.getStatus()
        );
    }

    private AuditLogResponse toAudit(AuditLogEntity log) {
        return new AuditLogResponse(
            log.getId(),
            log.getAction(),
            log.getActorUserId(),
            log.getResourceType(),
            log.getResourceId(),
            log.getCorrelationId(),
            log.getDetailsJson(),
            log.getCreatedAt()
        );
    }
}
