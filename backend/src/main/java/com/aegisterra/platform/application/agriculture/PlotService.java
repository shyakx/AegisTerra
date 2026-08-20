package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.PlotEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.PlotRepository;
import com.aegisterra.platform.application.contracts.PlotRequest;
import com.aegisterra.platform.application.contracts.PlotResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlotService {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository;
    private final GeometryService geometryService;
    private final AgricultureAuditHelper auditHelper;

    @PersistenceContext
    private EntityManager entityManager;

    public PlotService(
        PlotRepository plotRepository,
        FarmRepository farmRepository,
        GeometryService geometryService,
        AgricultureAuditHelper auditHelper
    ) {
        this.plotRepository = plotRepository;
        this.farmRepository = farmRepository;
        this.geometryService = geometryService;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<PlotResponse> listByFarm(UUID farmId) {
        return plotRepository.findByFarmIdAndDeletedFalse(farmId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlotResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public PlotResponse create(PlotRequest request, UUID actorId) {
        farmRepository.findByIdAndDeletedFalse(request.farmId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm not found"));
        boolean duplicate = plotRepository.findByFarmIdAndDeletedFalse(request.farmId()).stream()
            .anyMatch(p -> p.getPlotCode().equalsIgnoreCase(request.plotCode().trim()));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plot code already exists on this farm");
        }
        PlotEntity plot = new PlotEntity();
        plot.setFarmId(request.farmId());
        plot.setPlotCode(request.plotCode().trim());
        plot.setName(request.name());
        plot.setAreaHa(request.areaHa());
        plot.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status().trim().toUpperCase());
        plot.setDeleted(false);
        plot.setCreatedBy(actorId);
        if (request.geoJson() != null && !request.geoJson().isBlank()) {
            var result = geometryService.validateGeoJson(request.geoJson());
            if (!result.valid()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, result.reason());
            }
            plot.setGeom(result.geometry());
            plot.setAreaHa(result.areaHa());
        }
        plotRepository.save(plot);
        auditHelper.record(AuditAction.PLOT_CREATED, actorId, "plot", plot.getId(), null, toResponse(plot), request.reason());
        return toResponse(plot);
    }

    @Transactional
    public PlotResponse update(UUID id, PlotRequest request, UUID actorId) {
        PlotEntity plot = require(id);
        PlotResponse old = toResponse(plot);
        boolean duplicate = plotRepository.findByFarmIdAndDeletedFalse(plot.getFarmId()).stream()
            .anyMatch(p -> !p.getId().equals(id) && p.getPlotCode().equalsIgnoreCase(request.plotCode().trim()));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plot code already exists on this farm");
        }
        plot.setPlotCode(request.plotCode().trim());
        plot.setName(request.name());
        plot.setAreaHa(request.areaHa());
        if (request.status() != null && !request.status().isBlank()) {
            plot.setStatus(request.status().trim().toUpperCase());
        }
        if (request.geoJson() != null && !request.geoJson().isBlank()) {
            var result = geometryService.requireValidGeometry(request.geoJson());
            plot.setGeom(result);
            plot.setAreaHa(geometryService.validateGeoJson(request.geoJson()).areaHa());
        }
        plot.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.PLOT_UPDATED, actorId, "plot", id, old, toResponse(plot), request.reason());
        return toResponse(plot);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        PlotEntity plot = require(id);
        PlotResponse old = toResponse(plot);
        plot.setDeleted(true);
        plot.setStatus("INACTIVE");
        plot.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.PLOT_DELETED, actorId, "plot", id, old, toResponse(plot), "soft-delete");
    }

    private PlotEntity require(UUID id) {
        return plotRepository.findById(id)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found"));
    }

    private PlotResponse toResponse(PlotEntity plot) {
        String geoJson = null;
        if (plot.getGeom() != null && plot.getId() != null) {
            try {
                geoJson = (String) entityManager.createNativeQuery(
                    "SELECT ST_AsGeoJSON(geom) FROM plots WHERE id = :id"
                ).setParameter("id", plot.getId()).getSingleResult();
            } catch (Exception ignored) {
                geoJson = null;
            }
        }
        return new PlotResponse(
            plot.getId(),
            plot.getFarmId(),
            plot.getPlotCode(),
            plot.getName(),
            geoJson,
            plot.getAreaHa(),
            plot.getStatus()
        );
    }
}
