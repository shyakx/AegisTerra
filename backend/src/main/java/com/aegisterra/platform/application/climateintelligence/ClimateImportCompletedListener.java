package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ClimateImportCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(ClimateImportCompletedListener.class);

    private final RiskEngine riskEngine;
    private final FarmRepository farmRepository;

    public ClimateImportCompletedListener(RiskEngine riskEngine, FarmRepository farmRepository) {
        this.riskEngine = riskEngine;
        this.farmRepository = farmRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onImportCompleted(PlatformDomainEvent event) {
        if (!DomainEventTypes.CLIMATE_IMPORT_COMPLETED.equals(event.eventType())) {
            return;
        }
        Instant to = Instant.now();
        Instant from = to.minus(90, ChronoUnit.DAYS);
        UUID actorId = event.actorId();
        farmRepository.findAll().stream()
            .filter(f -> !f.isDeleted())
            .limit(10)
            .forEach(farm -> {
                try {
                    riskEngine.recalculateFarm(farm.getId(), from, to, actorId);
                } catch (Exception ex) {
                    log.warn("Climate intel recalc after import failed for farm {}: {}", farm.getId(), ex.getMessage());
                }
            });
    }
}
