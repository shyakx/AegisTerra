package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.config.SecurityProperties;
import com.aegisterra.platform.domain.identity.SystemRoleIds;
import com.aegisterra.platform.infrastructure.persistence.identity.UserEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Local/demo bootstrap: one ACTIVE login per system role (except SYSTEM_ADMIN, which
 * {@link IdentityBootstrap} creates). Disabled whenever security bootstrap is off.
 */
@Component
@Order(200)
public class DemoRoleUsersBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoRoleUsersBootstrap.class);

    private record DemoUser(String username, String email, String displayName, UUID roleId) {}

    private static final List<DemoUser> DEMO_USERS = List.of(
        new DemoUser(
            "insurance.admin",
            "insurance.admin@aegisterra.local",
            "Insurance Administrator",
            SystemRoleIds.INSURANCE_ADMIN
        ),
        new DemoUser(
            "insurance.officer",
            "insurance.officer@aegisterra.local",
            "Insurance Officer",
            SystemRoleIds.INSURANCE_OFFICER
        ),
        new DemoUser(
            "fi.officer",
            "fi.officer@aegisterra.local",
            "Financial Institution Officer",
            SystemRoleIds.FI_OFFICER
        ),
        new DemoUser(
            "gov.analyst",
            "gov.analyst@aegisterra.local",
            "Government Analyst",
            SystemRoleIds.GOVERNMENT_ANALYST
        ),
        new DemoUser(
            "aggregator",
            "aggregator@aegisterra.local",
            "Aggregator Officer",
            SystemRoleIds.AGGREGATOR
        ),
        new DemoUser(
            "farmer.demo",
            "farmer.demo@aegisterra.local",
            "Jean Niyonzima",
            SystemRoleIds.FARMER
        ),
        new DemoUser(
            "auditor",
            "auditor@aegisterra.local",
            "Auditor",
            SystemRoleIds.AUDITOR
        ),
        new DemoUser(
            "support",
            "support@aegisterra.local",
            "Support Agent",
            SystemRoleIds.SUPPORT
        )
    );

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public DemoRoleUsersBootstrap(
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
        SecurityProperties.Bootstrap bootstrap = securityProperties.getBootstrap();
        if (!bootstrap.isEnabled() || !bootstrap.isDemoUsersEnabled()) {
            return;
        }

        String password = bootstrap.getDemoUsersPassword();
        if (!StringUtils.hasText(password)) {
            log.warn("Demo role users enabled but demo-users-password is unset — skipping");
            return;
        }

        String hash = passwordEncoder.encode(password);
        int created = 0;
        for (DemoUser demo : DEMO_USERS) {
            if (userRepository.existsByUsernameIgnoreCaseAndDeletedFalse(demo.username())) {
                continue;
            }
            if (userRepository.existsByEmailIgnoreCaseAndDeletedFalse(demo.email())) {
                log.warn("Skipping demo user '{}' — email already exists", demo.username());
                continue;
            }

            UserEntity user = UserEntity.createLocal(
                demo.username(),
                demo.email().toLowerCase(Locale.ROOT),
                hash,
                demo.displayName()
            );
            userRepository.save(user);
            userRoleRepository.save(UserRoleEntity.assign(user.getId(), demo.roleId(), null));
            created++;
        }

        if (created > 0) {
            log.info("Bootstrapped {} operator accounts", created);
        }

        userRepository.findByUsernameIgnoreCaseAndDeletedFalse("farmer.demo").ifPresent(user -> {
            if (user.getDisplayName() == null || "Demo Farmer".equals(user.getDisplayName())) {
                user.setDisplayName("Jean Niyonzima");
                userRepository.save(user);
            }
        });
    }
}
