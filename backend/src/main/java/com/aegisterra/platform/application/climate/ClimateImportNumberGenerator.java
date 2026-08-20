package com.aegisterra.platform.application.climate;

import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ClimateImportNumberGenerator {

    public String next() {
        return "CLIM-IMP-" + Year.now().getValue() + "-"
            + String.format("%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L));
    }
}
