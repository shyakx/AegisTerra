package com.aegisterra.platform.application.claims.spi;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface WeatherVerificationPort {

    Map<String, Object> verify(UUID farmId, LocalDate incidentDate, String causeOfLoss);
}
