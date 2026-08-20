package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.HouseholdEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.HouseholdRepository;
import com.aegisterra.platform.application.contracts.HouseholdRequest;
import com.aegisterra.platform.application.contracts.HouseholdResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final AgricultureAuditHelper auditHelper;

    public HouseholdService(HouseholdRepository householdRepository, AgricultureAuditHelper auditHelper) {
        this.householdRepository = householdRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<HouseholdResponse> search(String q, String status, Pageable pageable) {
        return PageResponse.from(householdRepository.search(blankToNull(q), blankToNull(status), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public HouseholdResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public HouseholdResponse create(HouseholdRequest request, UUID actorId) {
        householdRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Household code already exists");
        });
        HouseholdEntity entity = new HouseholdEntity();
        entity.setCode(request.code().trim());
        entity.setHeadName(request.headName());
        entity.setStatus("ACTIVE");
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        householdRepository.save(entity);
        auditHelper.record(AuditAction.HOUSEHOLD_CREATED, actorId, "household", entity.getId(), null, toResponse(entity), null);
        return toResponse(entity);
    }

    @Transactional
    public HouseholdResponse update(UUID id, HouseholdRequest request, UUID actorId) {
        HouseholdEntity entity = require(id);
        HouseholdResponse old = toResponse(entity);
        householdRepository.findByCodeAndDeletedFalse(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Household code already exists");
            });
        entity.setCode(request.code().trim());
        entity.setHeadName(request.headName());
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.HOUSEHOLD_UPDATED, actorId, "household", id, old, toResponse(entity), null);
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        HouseholdEntity entity = require(id);
        HouseholdResponse old = toResponse(entity);
        entity.setDeleted(true);
        entity.setStatus("DELETED");
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.HOUSEHOLD_UPDATED, actorId, "household", id, old, toResponse(entity), "soft-delete");
    }

    private HouseholdEntity require(UUID id) {
        return householdRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Household not found"));
    }

    private HouseholdResponse toResponse(HouseholdEntity entity) {
        return new HouseholdResponse(entity.getId(), entity.getCode(), entity.getHeadName(), entity.getStatus());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
