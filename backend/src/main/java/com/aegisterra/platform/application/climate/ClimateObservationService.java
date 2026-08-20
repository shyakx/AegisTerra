package com.aegisterra.platform.application.climate;

import com.aegisterra.platform.domain.climate.ClimateVariableCodes;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationRepository;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.WeatherObservationResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClimateObservationService {

    private final WeatherObservationRepository observationRepository;

    public ClimateObservationService(WeatherObservationRepository observationRepository) {
        this.observationRepository = observationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<WeatherObservationResponse> search(
        UUID stationId,
        Instant from,
        Instant to,
        String variableCode,
        String qualityFlag,
        Pageable pageable
    ) {
        requireWindow(from, to);
        String variable = variableCode == null ? null : ClimateVariableCodes.canonical(variableCode);
        return PageResponse.from(observationRepository
            .search(stationId, from, to, variable, blankToNull(qualityFlag), pageable)
            .map(this::toResponse));
    }

    public WeatherObservationResponse toResponse(WeatherObservationEntity o) {
        return new WeatherObservationResponse(
            o.getId(),
            o.getStationId(),
            o.getObservedAt(),
            o.getVariableCode(),
            o.getValue(),
            o.getUnit(),
            o.getQualityFlag(),
            o.getProviderCode(),
            o.getTemperatureC(),
            o.getRainfallMm(),
            o.getHumidityPct(),
            o.getWindSpeedMs()
        );
    }

    public static void requireWindow(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to time window are required");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
