package com.aegisterra.platform.application.settlement;

import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SettlementNumberGenerator {

    public String next() {
        return "SET-" + Year.now().getValue() + "-"
            + String.format("%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L));
    }
}
