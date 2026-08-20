package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoverageLimitEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoverageLimitRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoveragePackageEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoveragePackageRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyExclusionEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyExclusionRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.WaitingPeriodEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.WaitingPeriodRepository;
import com.aegisterra.platform.application.contracts.CoverageLimitResponse;
import com.aegisterra.platform.application.contracts.CoveragePackageRequest;
import com.aegisterra.platform.application.contracts.CoveragePackageResponse;
import com.aegisterra.platform.application.contracts.ExclusionResponse;
import com.aegisterra.platform.application.contracts.WaitingPeriodResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CoveragePackageService {

    private final CoveragePackageRepository packageRepository;
    private final CoverageLimitRepository limitRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final WaitingPeriodRepository waitingPeriodRepository;
    private final InsuranceProductRepository productRepository;
    private final InsuranceAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public CoveragePackageService(
        CoveragePackageRepository packageRepository,
        CoverageLimitRepository limitRepository,
        PolicyExclusionRepository exclusionRepository,
        WaitingPeriodRepository waitingPeriodRepository,
        InsuranceProductRepository productRepository,
        InsuranceAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.packageRepository = packageRepository;
        this.limitRepository = limitRepository;
        this.exclusionRepository = exclusionRepository;
        this.waitingPeriodRepository = waitingPeriodRepository;
        this.productRepository = productRepository;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CoveragePackageResponse> listByProduct(UUID productId) {
        return packageRepository.findByProductIdAndDeletedFalseOrderByNameAsc(productId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CoveragePackageResponse get(UUID id) {
        return toResponse(require(id));
    }

    /** Frozen configuration snapshot for policy issuance. */
    @Transactional(readOnly = true)
    public String buildCoverageSnapshot(UUID packageId) {
        CoveragePackageResponse pkg = get(packageId);
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("package", pkg);
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to snapshot coverage");
        }
    }

    @Transactional
    public CoveragePackageResponse create(CoveragePackageRequest request, UUID actorId) {
        productRepository.findByIdAndDeletedFalse(request.productId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        packageRepository.findByProductIdAndCodeAndDeletedFalse(request.productId(), request.code()).ifPresent(x -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package code already exists for product");
        });
        CoveragePackageEntity entity = new CoveragePackageEntity();
        apply(entity, request);
        entity.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status().trim().toUpperCase());
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        packageRepository.save(entity);
        saveChildren(entity.getId(), request, actorId);
        auditHelper.record(AuditAction.COVERAGE_PACKAGE_CREATED, actorId, "coverage_package", entity.getId(), null, toResponse(entity), null);
        return toResponse(entity);
    }

    @Transactional
    public CoveragePackageResponse update(UUID id, CoveragePackageRequest request, UUID actorId) {
        CoveragePackageEntity entity = require(id);
        CoveragePackageResponse old = toResponse(entity);
        apply(entity, request);
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        entity.setUpdatedBy(actorId);
        limitRepository.findByCoveragePackageIdAndDeletedFalse(id).forEach(l -> {
            l.setDeleted(true);
            l.setUpdatedBy(actorId);
        });
        exclusionRepository.findByCoveragePackageIdAndDeletedFalse(id).forEach(e -> {
            e.setDeleted(true);
            e.setUpdatedBy(actorId);
        });
        waitingPeriodRepository.findByCoveragePackageIdAndDeletedFalse(id).forEach(w -> {
            w.setDeleted(true);
            w.setUpdatedBy(actorId);
        });
        saveChildren(id, request, actorId);
        auditHelper.record(AuditAction.COVERAGE_PACKAGE_UPDATED, actorId, "coverage_package", id, old, toResponse(entity), null);
        return toResponse(entity);
    }

    private void saveChildren(UUID packageId, CoveragePackageRequest request, UUID actorId) {
        if (request.limits() != null) {
            request.limits().forEach(limit -> {
                CoverageLimitEntity entity = new CoverageLimitEntity();
                entity.setCoveragePackageId(packageId);
                entity.setPerilCode(limit.perilCode());
                entity.setLimitAmount(limit.limitAmount());
                entity.setCurrency(limit.currency());
                entity.setStatus("ACTIVE");
                entity.setDeleted(false);
                entity.setCreatedBy(actorId);
                limitRepository.save(entity);
            });
        }
        if (request.exclusions() != null) {
            request.exclusions().forEach(ex -> {
                PolicyExclusionEntity entity = new PolicyExclusionEntity();
                entity.setCoveragePackageId(packageId);
                entity.setCode(ex.code());
                entity.setDescription(ex.description());
                entity.setStatus("ACTIVE");
                entity.setDeleted(false);
                entity.setCreatedBy(actorId);
                exclusionRepository.save(entity);
            });
        }
        if (request.waitingPeriods() != null) {
            request.waitingPeriods().forEach(wp -> {
                WaitingPeriodEntity entity = new WaitingPeriodEntity();
                entity.setCoveragePackageId(packageId);
                entity.setCode(wp.code());
                entity.setDays(wp.days());
                entity.setDescription(wp.description());
                entity.setStatus("ACTIVE");
                entity.setDeleted(false);
                entity.setCreatedBy(actorId);
                waitingPeriodRepository.save(entity);
            });
        }
    }

    private void apply(CoveragePackageEntity entity, CoveragePackageRequest request) {
        entity.setProductId(request.productId());
        entity.setPolicyTypeId(request.policyTypeId());
        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        entity.setCoverageLevelPct(request.coverageLevelPct());
        entity.setMaxSumInsured(request.maxSumInsured());
        entity.setConfigJson(request.configJson());
    }

    private CoveragePackageEntity require(UUID id) {
        return packageRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coverage package not found"));
    }

    private CoveragePackageResponse toResponse(CoveragePackageEntity entity) {
        return new CoveragePackageResponse(
            entity.getId(),
            entity.getProductId(),
            entity.getPolicyTypeId(),
            entity.getCode(),
            entity.getName(),
            entity.getCoverageLevelPct(),
            entity.getMaxSumInsured(),
            entity.getConfigJson(),
            entity.getStatus(),
            limitRepository.findByCoveragePackageIdAndDeletedFalse(entity.getId()).stream()
                .map(l -> new CoverageLimitResponse(l.getId(), l.getPerilCode(), l.getLimitAmount(), l.getCurrency()))
                .toList(),
            exclusionRepository.findByCoveragePackageIdAndDeletedFalse(entity.getId()).stream()
                .map(e -> new ExclusionResponse(e.getId(), e.getCode(), e.getDescription()))
                .toList(),
            waitingPeriodRepository.findByCoveragePackageIdAndDeletedFalse(entity.getId()).stream()
                .map(w -> new WaitingPeriodResponse(w.getId(), w.getCode(), w.getDays(), w.getDescription()))
                .toList()
        );
    }
}
