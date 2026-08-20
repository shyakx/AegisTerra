package com.aegisterra.platform.application.climate.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ClimateDataProvider {

    String code();

    List<String> capabilities();

    void validateConfig(Map<String, Object> config);

    default ProviderBatch fetchStationObservations(ProviderRequest request) {
        throw new UnsupportedOperationException(code() + " station observation fetch is stubbed");
    }

    default ProviderBatch fetchSatelliteProducts(ProviderRequest request) {
        throw new UnsupportedOperationException(code() + " satellite fetch is stubbed");
    }

    default ProviderBatch fetchRasterLayers(ProviderRequest request) {
        throw new UnsupportedOperationException(code() + " raster fetch is stubbed");
    }

    default ProviderBatch listStations(ProviderRequest request) {
        throw new UnsupportedOperationException(code() + " station list is stubbed");
    }

    default boolean healthCheck() {
        return true;
    }

    record ProviderRequest(
        Instant from,
        Instant to,
        String stationCode,
        Map<String, Object> params
    ) {}

    record ProviderBatch(
        String providerCode,
        int rowsRead,
        List<Map<String, Object>> rows,
        String message
    ) {}
}
