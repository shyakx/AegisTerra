package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.SeasonEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.SeasonRepository;
import com.aegisterra.platform.application.contracts.SeasonRequest;
import com.aegisterra.platform.application.contracts.SeasonResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final AgricultureAuditHelper auditHelper;

    public SeasonService(SeasonRepository seasonRepository, AgricultureAuditHelper auditHelper) {
        this.seasonRepository = seasonRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> list() {
        return seasonRepository.findAll().stream()
            .filter(s -> !s.isDeleted())
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SeasonResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public SeasonResponse create(SeasonRequest request, UUID actorId) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Season endDate must be on or after startDate");
        }
        seasonRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Season code already exists");
        });
        SeasonEntity season = new SeasonEntity();
        season.setCode(request.code().trim().toUpperCase());
        season.setName(request.name().trim());
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season.setStatus(request.status() == null || request.status().isBlank() ? "PLANNED" : request.status().trim().toUpperCase());
        season.setDeleted(false);
        season.setCreatedBy(actorId);
        seasonRepository.save(season);
        auditHelper.record(AuditAction.SEASON_CREATED, actorId, "season", season.getId(), null, toResponse(season), null);
        return toResponse(season);
    }

    @Transactional
    public SeasonResponse update(UUID id, SeasonRequest request, UUID actorId) {
        SeasonEntity season = require(id);
        SeasonResponse old = toResponse(season);
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Season endDate must be on or after startDate");
        }
        seasonRepository.findByCodeAndDeletedFalse(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Season code already exists");
            });
        season.setCode(request.code().trim().toUpperCase());
        season.setName(request.name().trim());
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        if (request.status() != null && !request.status().isBlank()) {
            season.setStatus(request.status().trim().toUpperCase());
        }
        season.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.SEASON_UPDATED, actorId, "season", id, old, toResponse(season), null);
        return toResponse(season);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        SeasonEntity season = require(id);
        SeasonResponse old = toResponse(season);
        season.setDeleted(true);
        season.setStatus("CLOSED");
        season.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.SEASON_UPDATED, actorId, "season", id, old, toResponse(season), "soft-delete");
    }

    private SeasonEntity require(UUID id) {
        return seasonRepository.findById(id)
            .filter(s -> !s.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found"));
    }

    private SeasonResponse toResponse(SeasonEntity season) {
        return new SeasonResponse(
            season.getId(),
            season.getCode(),
            season.getName(),
            season.getStartDate(),
            season.getEndDate(),
            season.getStatus()
        );
    }
}
