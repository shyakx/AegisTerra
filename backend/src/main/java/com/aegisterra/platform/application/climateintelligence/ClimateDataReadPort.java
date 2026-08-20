package com.aegisterra.platform.application.climateintelligence;

import com.aegisterra.platform.domain.climate.ClimateVariableCodes;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherObservationRepository;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption layer: 8B reads Climate Data facts only. Never imports climate.spi.
 */
@Component
public class ClimateDataReadPort {

    private final WeatherObservationRepository observationRepository;
    private final WeatherStationRepository stationRepository;

    public ClimateDataReadPort(
        WeatherObservationRepository observationRepository,
        WeatherStationRepository stationRepository
    ) {
        this.observationRepository = observationRepository;
        this.stationRepository = stationRepository;
    }

    public Optional<WeatherStationEntity> findStation(UUID stationId) {
        return stationRepository.findByIdAndDeletedFalse(stationId);
    }

    public List<WeatherStationEntity> stations() {
        return stationRepository.findByDeletedFalse();
    }

    public List<WeatherObservationEntity> observations(UUID stationId, Instant from, Instant to) {
        return observationRepository.findByStationIdAndObservedAtBetweenOrderByObservedAtAsc(stationId, from, to);
    }

    public BigDecimal rainfallTotal(UUID stationId, Instant from, Instant to) {
        BigDecimal sum = observationRepository.sumVariable(stationId, ClimateVariableCodes.RAIN_MM, from, to);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public BigDecimal temperatureSumProxy(UUID stationId, Instant from, Instant to) {
        BigDecimal sum = observationRepository.sumVariable(stationId, ClimateVariableCodes.TEMP_C, from, to);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
