package com.aegisterra.platform.application.climate;

import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationRepository;
import com.aegisterra.platform.application.contracts.CreateWeatherStationRequest;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.WeatherStationResponse;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClimateStationService {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final WeatherStationRepository stationRepository;

    public ClimateStationService(WeatherStationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<WeatherStationResponse> search(String q, String status, Pageable pageable) {
        return PageResponse.from(stationRepository.search(blankToNull(q), blankToNull(status), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public WeatherStationResponse get(UUID id) {
        return toResponse(stationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found")));
    }

    @Transactional
    public WeatherStationResponse create(CreateWeatherStationRequest request, UUID actorId) {
        if (request.longitude() < -180 || request.longitude() > 180 || request.latitude() < -90 || request.latitude() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates out of bounds (EPSG:4326)");
        }
        stationRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(s -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Station code already exists");
        });
        WeatherStationEntity entity = new WeatherStationEntity();
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setGeom(GF.createPoint(new Coordinate(request.longitude(), request.latitude())));
        entity.setElevationM(request.elevationM());
        entity.setProviderCode(request.providerCode() == null ? "MANUAL" : request.providerCode());
        entity.setExternalStationId(request.externalStationId());
        entity.setDistrictCode(request.districtCode());
        entity.setCreatedBy(actorId);
        entity.setUpdatedBy(actorId);
        entity.setDeleted(false);
        entity.setStatus("ACTIVE");
        return toResponse(stationRepository.save(entity));
    }

    public WeatherStationResponse toResponse(WeatherStationEntity s) {
        Double lon = s.getGeom() == null ? null : s.getGeom().getX();
        Double lat = s.getGeom() == null ? null : s.getGeom().getY();
        return new WeatherStationResponse(
            s.getId(),
            s.getCode(),
            s.getName(),
            s.getElevationM(),
            s.getProviderCode(),
            s.getExternalStationId(),
            s.getDistrictCode(),
            lon,
            lat,
            s.getStatus()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
