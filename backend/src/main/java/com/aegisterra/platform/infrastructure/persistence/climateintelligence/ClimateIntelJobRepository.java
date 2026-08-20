package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateIntelJobRepository extends JpaRepository<ClimateIntelJobEntity, UUID> {
}
