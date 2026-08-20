package com.aegisterra.platform.application.claims.spi;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubWeatherVerificationPort implements WeatherVerificationPort {

    @Override
    public Map<String, Object> verify(UUID farmId, LocalDate incidentDate, String causeOfLoss) {
        return Map.of(
            "verified", true,
            "source", "STUB",
            "farmId", farmId == null ? "" : farmId.toString(),
            "incidentDate", incidentDate == null ? "" : incidentDate.toString()
        );
    }
}
