package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropSeasonEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropSeasonRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.PlotRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.SeasonRepository;
import com.aegisterra.platform.application.contracts.CropSeasonRequest;
import com.aegisterra.platform.application.contracts.CropSeasonResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CropSeasonService {

    private final CropSeasonRepository cropSeasonRepository;
    private final FarmRepository farmRepository;
    private final PlotRepository plotRepository;
    private final CropRepository cropRepository;
    private final SeasonRepository seasonRepository;
    private final AgricultureAuditHelper auditHelper;

    public CropSeasonService(
        CropSeasonRepository cropSeasonRepository,
        FarmRepository farmRepository,
        PlotRepository plotRepository,
        CropRepository cropRepository,
        SeasonRepository seasonRepository,
        AgricultureAuditHelper auditHelper
    ) {
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmRepository = farmRepository;
        this.plotRepository = plotRepository;
        this.cropRepository = cropRepository;
        this.seasonRepository = seasonRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<CropSeasonResponse> listByFarm(UUID farmId) {
        return cropSeasonRepository.findByFarmIdAndDeletedFalse(farmId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CropSeasonResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public CropSeasonResponse create(CropSeasonRequest request, UUID actorId) {
        validateRefs(request);
        ensureUnique(request, null);
        CropSeasonEntity entity = new CropSeasonEntity();
        entity.setFarmId(request.farmId());
        entity.setPlotId(request.plotId());
        entity.setCropId(request.cropId());
        entity.setSeasonId(request.seasonId());
        entity.setPlantedAreaHa(request.plantedAreaHa());
        entity.setStatus(request.status() == null || request.status().isBlank() ? "PLANNED" : request.status().trim().toUpperCase());
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        cropSeasonRepository.save(entity);
        auditHelper.record(AuditAction.CROP_SEASON_CREATED, actorId, "crop_season", entity.getId(), null, toResponse(entity), request.reason());
        return toResponse(entity);
    }

    @Transactional
    public CropSeasonResponse update(UUID id, CropSeasonRequest request, UUID actorId) {
        CropSeasonEntity entity = require(id);
        CropSeasonResponse old = toResponse(entity);
        validateRefs(request);
        ensureUnique(request, id);
        entity.setFarmId(request.farmId());
        entity.setPlotId(request.plotId());
        entity.setCropId(request.cropId());
        entity.setSeasonId(request.seasonId());
        entity.setPlantedAreaHa(request.plantedAreaHa());
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.CROP_SEASON_UPDATED, actorId, "crop_season", id, old, toResponse(entity), request.reason());
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        CropSeasonEntity entity = require(id);
        CropSeasonResponse old = toResponse(entity);
        entity.setDeleted(true);
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.CROP_SEASON_UPDATED, actorId, "crop_season", id, old, toResponse(entity), "soft-delete");
    }

    private void validateRefs(CropSeasonRequest request) {
        farmRepository.findByIdAndDeletedFalse(request.farmId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm not found"));
        cropRepository.findById(request.cropId()).filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crop not found"));
        seasonRepository.findById(request.seasonId()).filter(s -> !s.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Season not found"));
        if (request.plotId() != null) {
            plotRepository.findById(request.plotId()).filter(p -> !p.isDeleted() && p.getFarmId().equals(request.farmId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plot not found on farm"));
        }
    }

    private void ensureUnique(CropSeasonRequest request, UUID excludeId) {
        boolean duplicate = cropSeasonRepository.findByFarmIdAndDeletedFalse(request.farmId()).stream()
            .anyMatch(existing ->
                (excludeId == null || !existing.getId().equals(excludeId))
                    && existing.getSeasonId().equals(request.seasonId())
                    && existing.getCropId().equals(request.cropId())
                    && Objects.equals(existing.getPlotId(), request.plotId())
            );
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Crop season already exists for this farm/plot/crop/season");
        }
    }

    private CropSeasonEntity require(UUID id) {
        return cropSeasonRepository.findById(id)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Crop season not found"));
    }

    private CropSeasonResponse toResponse(CropSeasonEntity entity) {
        return new CropSeasonResponse(
            entity.getId(),
            entity.getFarmId(),
            entity.getPlotId(),
            entity.getCropId(),
            entity.getSeasonId(),
            entity.getPlantedAreaHa(),
            entity.getStatus()
        );
    }
}
