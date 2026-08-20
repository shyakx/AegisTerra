package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyTypeEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyTypeRepository;
import com.aegisterra.platform.application.contracts.InsuranceProductRequest;
import com.aegisterra.platform.application.contracts.InsuranceProductResponse;
import com.aegisterra.platform.application.contracts.PolicyTypeRequest;
import com.aegisterra.platform.application.contracts.PolicyTypeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InsuranceProductService {

    private final InsuranceProductRepository productRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final InsuranceAuditHelper auditHelper;

    public InsuranceProductService(
        InsuranceProductRepository productRepository,
        PolicyTypeRepository policyTypeRepository,
        InsuranceAuditHelper auditHelper
    ) {
        this.productRepository = productRepository;
        this.policyTypeRepository = policyTypeRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<InsuranceProductResponse> listProducts() {
        return productRepository.findByDeletedFalseOrderByNameAsc().stream().map(this::toProduct).toList();
    }

    @Transactional(readOnly = true)
    public InsuranceProductResponse getProduct(UUID id) {
        return toProduct(requireProduct(id));
    }

    @Transactional
    public InsuranceProductResponse createProduct(InsuranceProductRequest request, UUID actorId) {
        productRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(x -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product code already exists");
        });
        InsuranceProductEntity entity = new InsuranceProductEntity();
        applyProduct(entity, request);
        entity.setStatus(blank(request.status(), "ACTIVE"));
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        productRepository.save(entity);
        auditHelper.record(AuditAction.PRODUCT_CREATED, actorId, "insurance_product", entity.getId(), null, toProduct(entity), null);
        return toProduct(entity);
    }

    @Transactional
    public InsuranceProductResponse updateProduct(UUID id, InsuranceProductRequest request, UUID actorId) {
        InsuranceProductEntity entity = requireProduct(id);
        InsuranceProductResponse old = toProduct(entity);
        productRepository.findByCodeAndDeletedFalse(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(x -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Product code already exists");
            });
        applyProduct(entity, request);
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.PRODUCT_UPDATED, actorId, "insurance_product", id, old, toProduct(entity), null);
        return toProduct(entity);
    }

    @Transactional(readOnly = true)
    public List<PolicyTypeResponse> listPolicyTypes() {
        return policyTypeRepository.findAll().stream().filter(t -> !t.isDeleted()).map(this::toType).toList();
    }

    @Transactional
    public PolicyTypeResponse createPolicyType(PolicyTypeRequest request, UUID actorId) {
        policyTypeRepository.findByCodeAndDeletedFalse(request.code()).ifPresent(x -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policy type code already exists");
        });
        if (request.productId() != null) {
            requireProduct(request.productId());
        }
        PolicyTypeEntity entity = new PolicyTypeEntity();
        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCoverageRulesJson(request.coverageRulesJson());
        entity.setProductId(request.productId());
        entity.setStatus(blank(request.status(), "ACTIVE"));
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        policyTypeRepository.save(entity);
        auditHelper.record(AuditAction.POLICY_TYPE_CREATED, actorId, "policy_type", entity.getId(), null, toType(entity), null);
        return toType(entity);
    }

    @Transactional
    public PolicyTypeResponse updatePolicyType(UUID id, PolicyTypeRequest request, UUID actorId) {
        PolicyTypeEntity entity = policyTypeRepository.findById(id).filter(t -> !t.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy type not found"));
        PolicyTypeResponse old = toType(entity);
        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCoverageRulesJson(request.coverageRulesJson());
        entity.setProductId(request.productId());
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        entity.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.POLICY_TYPE_UPDATED, actorId, "policy_type", id, old, toType(entity), null);
        return toType(entity);
    }

    private void applyProduct(InsuranceProductEntity entity, InsuranceProductRequest request) {
        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setPricingStrategyCode(request.pricingStrategyCode().trim());
        entity.setEligibilityJson(request.eligibilityJson());
        entity.setConfigJson(request.configJson());
    }

    private InsuranceProductEntity requireProduct(UUID id) {
        return productRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private InsuranceProductResponse toProduct(InsuranceProductEntity e) {
        return new InsuranceProductResponse(e.getId(), e.getCode(), e.getName(), e.getDescription(),
            e.getPricingStrategyCode(), e.getEligibilityJson(), e.getConfigJson(), e.getStatus());
    }

    private PolicyTypeResponse toType(PolicyTypeEntity e) {
        return new PolicyTypeResponse(e.getId(), e.getCode(), e.getName(), e.getDescription(),
            e.getCoverageRulesJson(), e.getProductId(), e.getStatus());
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }
}
