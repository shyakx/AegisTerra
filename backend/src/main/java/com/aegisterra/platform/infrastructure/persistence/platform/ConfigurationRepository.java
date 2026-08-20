package com.aegisterra.platform.infrastructure.persistence.platform;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, UUID> {
    Optional<ConfigurationEntity> findByConfigKeyAndDeletedFalse(String configKey);
}
