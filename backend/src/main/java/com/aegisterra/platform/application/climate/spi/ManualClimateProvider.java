package com.aegisterra.platform.application.climate.spi;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ManualClimateProvider implements ClimateDataProvider {

    @Override
    public String code() {
        return "MANUAL";
    }

    @Override
    public List<String> capabilities() {
        return List.of("STATION_OBS");
    }

    @Override
    public void validateConfig(Map<String, Object> config) {
        // no external config required
    }

    @Override
    public ProviderBatch fetchStationObservations(ProviderRequest request) {
        return new ProviderBatch(code(), 0, List.of(), "Use ClimateImportService CSV/manual payload");
    }

    @Override
    public boolean healthCheck() {
        return true;
    }
}
