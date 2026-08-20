package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.config.SecurityProperties;
import com.aegisterra.platform.domain.identity.SystemRoleIds;
import com.aegisterra.platform.domain.identity.UserStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.UserEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleRepository;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(100)
public class IdentityBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityBootstrap.class);

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public IdentityBootstrap(
        UserRepository userRepository,
        UserRoleRepository userRoleRepository,
        PasswordEncoder passwordEncoder,
        SecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!securityProperties.getBootstrap().isEnabled()) {
            return;
        }
        if (userRepository.existsActiveUserWithRole(SystemRoleIds.SYSTEM_ADMIN)) {
            return;
        }

        String password = securityProperties.getBootstrap().getAdminPassword();
        if (!StringUtils.hasText(password)) {
            log.warn("Bootstrap enabled but AEGISTERRA_BOOTSTRAP_ADMIN_PASSWORD is unset — skipping");
            return;
        }

        SecurityProperties.Bootstrap bootstrap = securityProperties.getBootstrap();
        UserEntity admin = new UserEntity();
        admin.setUsername(bootstrap.getAdminUsername());
        admin.setEmail(bootstrap.getAdminEmail().toLowerCase(Locale.ROOT));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName("System Administrator");
        admin.setMustChangePassword(false);
        admin.setFailedLoginAttempts(0);
        admin.setTokenVersion(0);
        admin.setMfaEnabled(false);
        admin.setMfaEnforced(false);
        admin.setPasswordChangedAt(Instant.now());
        admin.setStatus(UserStatus.ACTIVE.name());
        admin.setDeleted(false);
        userRepository.save(admin);

        UserRoleEntity assignment = new UserRoleEntity();
        assignment.setUserId(admin.getId());
        assignment.setRoleId(SystemRoleIds.SYSTEM_ADMIN);
        assignment.setAssignedAt(Instant.now());
        assignment.setStatus("ACTIVE");
        assignment.setDeleted(false);
        userRoleRepository.save(assignment);

        log.info("Bootstrapped SYSTEM_ADMIN user '{}'", admin.getUsername());
    }
}
