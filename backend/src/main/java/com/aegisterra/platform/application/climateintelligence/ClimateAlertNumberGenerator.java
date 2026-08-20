package com.aegisterra.platform.application.climateintelligence;

import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ClimateAlertNumberGenerator {

    public String nextAlert() {
        return "CLT-ALRT-" + Year.now().getValue() + "-"
            + String.format("%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L));
    }

    public String nextJob() {
        return "CLT-JOB-" + Year.now().getValue() + "-"
            + String.format("%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L));
    }
}
