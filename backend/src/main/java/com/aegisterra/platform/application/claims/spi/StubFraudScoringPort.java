package com.aegisterra.platform.application.claims.spi;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubFraudScoringPort implements FraudScoringPort {

    @Override
    public FraudScore score(UUID claimId, UUID policyId, UUID farmerId) {
        return new FraudScore("LOW", BigDecimal.ZERO, List.of());
    }
}
