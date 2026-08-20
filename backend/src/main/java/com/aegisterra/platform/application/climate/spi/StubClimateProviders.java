package com.aegisterra.platform.application.climate.spi;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

abstract class StubClimateProvider implements ClimateDataProvider {

    private final String code;
    private final List<String> capabilities;

    protected StubClimateProvider(String code, List<String> capabilities) {
        this.code = code;
        this.capabilities = capabilities;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public List<String> capabilities() {
        return capabilities;
    }

    @Override
    public void validateConfig(Map<String, Object> config) {
        // stubs accept any config shape
    }

    @Override
    public boolean healthCheck() {
        return true;
    }
}

@Component
class NationalMetClimateProvider extends StubClimateProvider {
    NationalMetClimateProvider() { super("NATIONAL_MET", List.of("STATION_OBS")); }
}

@Component
class OpenWeatherClimateProvider extends StubClimateProvider {
    OpenWeatherClimateProvider() { super("OPENWEATHER", List.of("STATION_OBS", "GRID")); }
}

@Component
class TomorrowIoClimateProvider extends StubClimateProvider {
    TomorrowIoClimateProvider() { super("TOMORROW_IO", List.of("STATION_OBS", "GRID")); }
}

@Component
class NasaClimateProvider extends StubClimateProvider {
    NasaClimateProvider() { super("NASA", List.of("SATELLITE", "RASTER")); }
}

@Component
class CopernicusClimateProvider extends StubClimateProvider {
    CopernicusClimateProvider() { super("COPERNICUS", List.of("SATELLITE", "RASTER")); }
}

@Component
class SentinelClimateProvider extends StubClimateProvider {
    SentinelClimateProvider() { super("SENTINEL", List.of("SATELLITE")); }
}

@Component
class PlanetClimateProvider extends StubClimateProvider {
    PlanetClimateProvider() { super("PLANET", List.of("SATELLITE")); }
}

@Component
class NoaaClimateProvider extends StubClimateProvider {
    NoaaClimateProvider() { super("NOAA", List.of("STATION_OBS", "GRID")); }
}
