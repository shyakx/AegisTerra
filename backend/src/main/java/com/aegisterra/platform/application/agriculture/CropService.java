package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropRepository;
import com.aegisterra.platform.application.contracts.CropRequest;
import com.aegisterra.platform.application.contracts.CropResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CropService {

    private final CropRepository cropRepository;
    private final AgricultureAuditHelper auditHelper;

    public CropService(CropRepository cropRepository, AgricultureAuditHelper auditHelper) {
        this.cropRepository = cropRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<CropResponse> list() {
        return cropRepository.findByDeletedFalseOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CropResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public CropResponse create(CropRequest request, UUID actorId) {
        cropRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Crop code already exists");
        });
        CropEntity crop = new CropEntity();
        crop.setCode(request.code().trim().toUpperCase());
        crop.setName(request.name().trim());
        crop.setScientificName(request.scientificName());
        crop.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status().trim().toUpperCase());
        crop.setDeleted(false);
        crop.setCreatedBy(actorId);
        cropRepository.save(crop);
        auditHelper.record(AuditAction.CROP_CREATED, actorId, "crop", crop.getId(), null, toResponse(crop), null);
        return toResponse(crop);
    }

    @Transactional
    public CropResponse update(UUID id, CropRequest request, UUID actorId) {
        CropEntity crop = require(id);
        CropResponse old = toResponse(crop);
        cropRepository.findByCodeAndDeletedFalse(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Crop code already exists");
            });
        crop.setCode(request.code().trim().toUpperCase());
        crop.setName(request.name().trim());
        crop.setScientificName(request.scientificName());
        if (request.status() != null && !request.status().isBlank()) {
            crop.setStatus(request.status().trim().toUpperCase());
        }
        crop.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.CROP_UPDATED, actorId, "crop", id, old, toResponse(crop), null);
        return toResponse(crop);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        CropEntity crop = require(id);
        CropResponse old = toResponse(crop);
        crop.setDeleted(true);
        crop.setStatus("DISABLED");
        crop.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.CROP_UPDATED, actorId, "crop", id, old, toResponse(crop), "soft-delete");
    }

    private CropEntity require(UUID id) {
        return cropRepository.findById(id)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Crop not found"));
    }

    private CropResponse toResponse(CropEntity crop) {
        return new CropResponse(crop.getId(), crop.getCode(), crop.getName(), crop.getScientificName(), crop.getStatus());
    }
}
