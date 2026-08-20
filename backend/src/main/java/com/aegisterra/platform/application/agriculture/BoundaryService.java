package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.application.contracts.FarmBoundaryRequest;
import com.aegisterra.platform.application.contracts.FarmBoundaryResponse;
import com.aegisterra.platform.application.contracts.GeometryValidateRequest;
import com.aegisterra.platform.application.contracts.GeometryValidateResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoundaryService {

    private final FarmBoundaryRepository farmBoundaryRepository;
    private final FarmRepository farmRepository;
    private final GeometryService geometryService;
    private final AgricultureAuditHelper auditHelper;

    @PersistenceContext
    private EntityManager entityManager;

    public BoundaryService(
        FarmBoundaryRepository farmBoundaryRepository,
        FarmRepository farmRepository,
        GeometryService geometryService,
        AgricultureAuditHelper auditHelper
    ) {
        this.farmBoundaryRepository = farmBoundaryRepository;
        this.farmRepository = farmRepository;
        this.geometryService = geometryService;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<FarmBoundaryResponse> listByFarm(UUID farmId) {
        return farmBoundaryRepository.findByFarmIdAndDeletedFalse(farmId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FarmBoundaryResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public GeometryValidateResponse validate(GeometryValidateRequest request) {
        var result = geometryService.validateGeoJson(request.geoJson());
        return new GeometryValidateResponse(result.valid(), result.reason(), result.areaHa());
    }

    @Transactional
    public FarmBoundaryResponse create(FarmBoundaryRequest request, UUID actorId) {
        farmRepository.findByIdAndDeletedFalse(request.farmId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm not found"));
        var validation = geometryService.validateGeoJson(request.geoJson());
        String desiredStatus = request.status() == null || request.status().isBlank()
            ? "DRAFT"
            : request.status().trim().toUpperCase();
        if ("ACTIVE".equals(desiredStatus) && !validation.valid()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                validation.reason() != null ? validation.reason() : "Invalid geometry");
        }
        if (!"ACTIVE".equals(desiredStatus) && !validation.valid()) {
            // allow DRAFT only when geometry is still parseable; reject invalid/self-intersecting always
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                validation.reason() != null ? validation.reason() : "Invalid geometry");
        }

        FarmBoundaryEntity boundary = FarmBoundaryEntity.create(request.farmId(), validation.geometry());
        boundary.setSource(request.source() == null ? "MAPLIBRE" : request.source());
        boundary.setAreaHa(validation.areaHa());
        boundary.setCreatedBy(actorId);
        boundary.setStatus("DRAFT");
        farmBoundaryRepository.save(boundary);

        if ("ACTIVE".equals(desiredStatus)) {
            activateInternal(boundary, actorId, request.reason());
        }

        auditHelper.record(AuditAction.FARM_BOUNDARY_CREATED, actorId, "farm_boundary", boundary.getId(),
            null, toResponse(boundary), request.reason());
        return toResponse(boundary);
    }

    @Transactional
    public FarmBoundaryResponse update(UUID id, FarmBoundaryRequest request, UUID actorId) {
        FarmBoundaryEntity boundary = require(id);
        FarmBoundaryResponse old = toResponse(boundary);
        var validation = geometryService.requireValidGeometry(request.geoJson());
        var result = geometryService.validateGeoJson(request.geoJson());
        boundary.setGeom(validation);
        boundary.setAreaHa(result.areaHa());
        boundary.setSource(request.source() == null ? boundary.getSource() : request.source());
        boundary.setUpdatedBy(actorId);

        String desiredStatus = request.status() == null || request.status().isBlank()
            ? boundary.getStatus()
            : request.status().trim().toUpperCase();
        if ("ACTIVE".equals(desiredStatus) && !"ACTIVE".equals(boundary.getStatus())) {
            activateInternal(boundary, actorId, request.reason());
        } else if ("ARCHIVED".equals(desiredStatus)) {
            boundary.setStatus("ARCHIVED");
        }

        auditHelper.record(AuditAction.FARM_BOUNDARY_UPDATED, actorId, "farm_boundary", id, old, toResponse(boundary), request.reason());
        return toResponse(boundary);
    }

    @Transactional
    public FarmBoundaryResponse activate(UUID id, UUID actorId, String reason) {
        FarmBoundaryEntity boundary = require(id);
        if (boundary.getGeom() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Boundary geometry is required");
        }
        Boolean valid = farmBoundaryRepository.isGeometryValid(id);
        if (!Boolean.TRUE.equals(valid)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Boundary geometry is invalid");
        }
        FarmBoundaryResponse old = toResponse(boundary);
        activateInternal(boundary, actorId, reason);
        auditHelper.record(AuditAction.FARM_BOUNDARY_ACTIVATED, actorId, "farm_boundary", id, old, toResponse(boundary), reason);
        return toResponse(boundary);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        FarmBoundaryEntity boundary = require(id);
        FarmBoundaryResponse old = toResponse(boundary);
        boundary.setDeleted(true);
        boundary.setStatus("ARCHIVED");
        boundary.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.FARM_BOUNDARY_UPDATED, actorId, "farm_boundary", id, old, toResponse(boundary), "soft-delete");
    }

    private void activateInternal(FarmBoundaryEntity boundary, UUID actorId, String reason) {
        farmBoundaryRepository.findByFarmIdAndDeletedFalse(boundary.getFarmId()).stream()
            .filter(existing -> "ACTIVE".equals(existing.getStatus()) && !existing.getId().equals(boundary.getId()))
            .forEach(existing -> {
                existing.setStatus("ARCHIVED");
                existing.setUpdatedBy(actorId);
            });
        boundary.setStatus("ACTIVE");
        boundary.setUpdatedBy(actorId);
        // future-ready overlap detection hook
        // TODO: ST_Intersects against neighboring ACTIVE boundaries
    }

    private FarmBoundaryEntity require(UUID id) {
        return farmBoundaryRepository.findById(id)
            .filter(b -> !b.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm boundary not found"));
    }

    private FarmBoundaryResponse toResponse(FarmBoundaryEntity boundary) {
        String geoJson = null;
        if (boundary.getGeom() != null) {
            geoJson = (String) entityManager.createNativeQuery(
                "SELECT ST_AsGeoJSON(geom) FROM farm_boundaries WHERE id = :id"
            ).setParameter("id", boundary.getId()).getSingleResult();
        }
        return new FarmBoundaryResponse(
            boundary.getId(),
            boundary.getFarmId(),
            geoJson,
            boundary.getSource(),
            boundary.getCapturedAt(),
            boundary.getAreaHa(),
            boundary.getStatus()
        );
    }
}
