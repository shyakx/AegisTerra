package com.aegisterra.platform.application.claims.spi;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubSatelliteVerificationPort implements SatelliteVerificationPort {

    @Override
    public Map<String, Object> verify(UUID farmId, LocalDate incidentDate) {
        return Map.of(
            "verified", true,
            "source", "STUB",
            "farmId", farmId == null ? "" : farmId.toString(),
            "incidentDate", incidentDate == null ? "" : incidentDate.toString()
        );
    }
}
