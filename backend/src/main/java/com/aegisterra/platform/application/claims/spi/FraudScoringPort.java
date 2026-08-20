package com.aegisterra.platform.application.claims.spi;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FraudScoringPort {

    FraudScore score(UUID claimId, UUID policyId, UUID farmerId);

    record FraudScore(String tier, BigDecimal score, List<String> flags) {}
}
