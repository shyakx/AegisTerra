package com.aegisterra.platform.application.claims;

import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ClaimNumberGenerator {

    public String next() {
        return "CLM-" + Year.now().getValue() + "-"
            + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000));
    }
}
