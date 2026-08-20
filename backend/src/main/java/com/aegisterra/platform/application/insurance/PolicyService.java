package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.insurance.PolicyStatus;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoveragePackageRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyHistoryEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyHistoryRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyBeneficiaryEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyBeneficiaryRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyTypeRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumQuoteEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumRepository;
import com.aegisterra.platform.application.contracts.BeneficiaryRequest;
import com.aegisterra.platform.application.contracts.BeneficiaryResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.PolicyResponse;
import com.aegisterra.platform.application.contracts.PolicySubmitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyService {

    private final InsurancePolicyRepository policyRepository;
    private final InsurancePolicyHistoryRepository historyRepository;
    private final PremiumRepository premiumRepository;
    private final PolicyBeneficiaryRepository beneficiaryRepository;
    private final FarmerRepository farmerRepository;
    private final FarmRepository farmRepository;
    private final FarmBoundaryRepository farmBoundaryRepository;
    private final InsuranceProductRepository productRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final CoveragePackageRepository packageRepository;
    private final PremiumPricingEngine pricingEngine;
    private final CoveragePackageService coveragePackageService;
    private final PolicyDocumentService documentService;
    private final InsuranceAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public PolicyService(
        InsurancePolicyRepository policyRepository,
        InsurancePolicyHistoryRepository historyRepository,
        PremiumRepository premiumRepository,
        PolicyBeneficiaryRepository beneficiaryRepository,
        FarmerRepository farmerRepository,
        FarmRepository farmRepository,
        FarmBoundaryRepository farmBoundaryRepository,
        InsuranceProductRepository productRepository,
        PolicyTypeRepository policyTypeRepository,
        CoveragePackageRepository packageRepository,
        PremiumPricingEngine pricingEngine,
        CoveragePackageService coveragePackageService,
        PolicyDocumentService documentService,
        InsuranceAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.policyRepository = policyRepository;
        this.historyRepository = historyRepository;
        this.premiumRepository = premiumRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.farmerRepository = farmerRepository;
        this.farmRepository = farmRepository;
        this.farmBoundaryRepository = farmBoundaryRepository;
        this.productRepository = productRepository;
        this.policyTypeRepository = policyTypeRepository;
        this.packageRepository = packageRepository;
        this.pricingEngine = pricingEngine;
        this.coveragePackageService = coveragePackageService;
        this.documentService = documentService;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> search(
        String q, String policyNumber, UUID farmerId, UUID farmId, UUID productId,
        UUID seasonId, UUID cropId, UUID companyId, String status, Pageable pageable
    ) {
        return PageResponse.from(policyRepository.search(
            blank(q), blank(policyNumber), farmerId, farmId, productId, seasonId, cropId, companyId, blank(status), pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(String q, String status) {
        var page = policyRepository.search(blank(q), null, null, null, null, null, null, null, blank(status),
            org.springframework.data.domain.PageRequest.of(0, 10_000));
        return page.getContent().stream()
            .map(p -> String.join(",",
                csv(p.getPolicyNumber()), csv(p.getStatus()), csv(p.getFarmerId().toString()),
                csv(p.getFarmId().toString()), csv(p.getPremiumAmount().toPlainString()),
                csv(p.getCoverageAmount().toPlainString())))
            .collect(Collectors.joining("\n", "policyNumber,status,farmerId,farmId,premium,coverage\n", "\n"));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listBeneficiaries(UUID policyId) {
        require(policyId);
        return beneficiaryRepository.findByPolicyIdAndDeletedFalse(policyId).stream()
            .map(b -> new BeneficiaryResponse(b.getId(), b.getPolicyId(), b.getFullName(), b.getRelationship(), b.getNationalId(), b.getSharePct()))
            .toList();
    }

    @Transactional
    public PolicyResponse submit(PolicySubmitRequest request, UUID actorId) {
        validateRefs(request);
        PremiumQuoteEntity quote = pricingEngine.requireValidQuote(request.premiumQuoteId());
        if (!quote.getFarmerId().equals(request.farmerId()) || !quote.getFarmId().equals(request.farmId())
            || !quote.getProductId().equals(request.productId())
            || !quote.getCoveragePackageId().equals(request.coveragePackageId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Quote does not match policy selection");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on/after startDate");
        }

        InsurancePolicyEntity policy = new InsurancePolicyEntity();
        policy.setPolicyNumber(nextPolicyNumber());
        policy.setFarmerId(request.farmerId());
        policy.setFarmId(request.farmId());
        policy.setPolicyTypeId(request.policyTypeId());
        policy.setProductId(request.productId());
        policy.setCoveragePackageId(request.coveragePackageId());
        policy.setCropId(request.cropId());
        policy.setSeasonId(request.seasonId());
        policy.setCropSeasonId(request.cropSeasonId());
        policy.setPremiumQuoteId(quote.getId());
        policy.setInsuranceCompanyId(request.insuranceCompanyId());
        policy.setCoverageAmount(quote.getCoverageAmount());
        policy.setPremiumAmount(quote.getNetAmount());
        policy.setCurrency(quote.getCurrency());
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());
        policy.setCoverageSnapshotJson(coveragePackageService.buildCoverageSnapshot(request.coveragePackageId()));
        policy.setStatus(PolicyStatus.DRAFT.name());
        policy.setDeleted(false);
        policy.setCreatedBy(actorId);
        policyRepository.save(policy);

        if (request.beneficiaries() != null) {
            request.beneficiaries().forEach(b -> saveBeneficiary(policy.getId(), b, actorId));
        }

        transition(policy, PolicyStatus.SUBMITTED, actorId, request.reason(), AuditAction.POLICY_SUBMITTED);
        transition(policy, PolicyStatus.UNDER_REVIEW, actorId, "auto-review-queue", AuditAction.POLICY_UNDER_REVIEW);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse approve(UUID id, UUID actorId, String reason) {
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.APPROVED, actorId, reason, AuditAction.POLICY_APPROVED);
        transition(policy, PolicyStatus.PREMIUM_PENDING, actorId, "premium-schedule", AuditAction.POLICY_PREMIUM_PENDING);
        PremiumEntity premium = new PremiumEntity();
        premium.setPolicyId(policy.getId());
        premium.setAmount(policy.getPremiumAmount());
        premium.setCurrency(policy.getCurrency());
        premium.setDueDate(LocalDate.now().plusDays(14));
        premium.setStatus("DUE");
        premium.setDeleted(false);
        premium.setCreatedBy(actorId);
        premiumRepository.save(premium);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse reject(UUID id, UUID actorId, String reason) {
        requireReason(reason);
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.REJECTED, actorId, reason, AuditAction.POLICY_REJECTED);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse markPremiumPaid(UUID id, UUID actorId, String reason) {
        InsurancePolicyEntity policy = require(id);
        PolicyStatus.parse(policy.getStatus()).assertCanTransitionTo(PolicyStatus.ACTIVE); // ensure path includes PREMIUM_PENDING
        if (!PolicyStatus.PREMIUM_PENDING.name().equals(policy.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Premium can only be marked paid in PREMIUM_PENDING");
        }
        premiumRepository.findByPolicyIdAndDeletedFalse(policy.getId()).forEach(p -> {
            p.setStatus("PAID");
            p.setPaidAt(Instant.now());
            p.setUpdatedBy(actorId);
        });
        return activate(id, actorId, reason == null ? "premium-paid" : reason);
    }

    @Transactional
    public PolicyResponse activate(UUID id, UUID actorId, String reason) {
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.ACTIVE, actorId, reason, AuditAction.POLICY_ACTIVATED);
        policy.setIssuedAt(Instant.now());
        documentService.generateAll(policy, actorId);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse suspend(UUID id, UUID actorId, String reason) {
        requireReason(reason);
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.SUSPENDED, actorId, reason, AuditAction.POLICY_SUSPENDED);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse reinstate(UUID id, UUID actorId, String reason) {
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.ACTIVE, actorId, reason, AuditAction.POLICY_REINSTATED);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse cancel(UUID id, UUID actorId, String reason) {
        requireReason(reason);
        InsurancePolicyEntity policy = require(id);
        transition(policy, PolicyStatus.CANCELLED, actorId, reason, AuditAction.POLICY_CANCELLED);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse renew(UUID id, UUID actorId, String reason) {
        InsurancePolicyEntity parent = require(id);
        if (!PolicyStatus.EXPIRED.name().equals(parent.getStatus()) && !PolicyStatus.ACTIVE.name().equals(parent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only ACTIVE/EXPIRED policies can start renewal");
        }
        if (PolicyStatus.ACTIVE.name().equals(parent.getStatus())) {
            transition(parent, PolicyStatus.EXPIRED, actorId, "renewal-supersede", AuditAction.POLICY_EXPIRED);
        }
        InsurancePolicyEntity child = new InsurancePolicyEntity();
        child.setPolicyNumber(nextPolicyNumber());
        child.setFarmerId(parent.getFarmerId());
        child.setFarmId(parent.getFarmId());
        child.setPolicyTypeId(parent.getPolicyTypeId());
        child.setProductId(parent.getProductId());
        child.setCoveragePackageId(parent.getCoveragePackageId());
        child.setCropId(parent.getCropId());
        child.setSeasonId(parent.getSeasonId());
        child.setCropSeasonId(parent.getCropSeasonId());
        child.setParentPolicyId(parent.getId());
        child.setInsuranceCompanyId(parent.getInsuranceCompanyId());
        child.setCoverageAmount(parent.getCoverageAmount());
        child.setPremiumAmount(parent.getPremiumAmount());
        child.setCurrency(parent.getCurrency());
        child.setStartDate(parent.getEndDate().plusDays(1));
        child.setEndDate(parent.getEndDate().plusDays(1).plusYears(1));
        child.setCoverageSnapshotJson(parent.getCoverageSnapshotJson());
        child.setStatus(PolicyStatus.DRAFT.name());
        child.setDeleted(false);
        child.setCreatedBy(actorId);
        child.setTransitionReason(reason);
        policyRepository.save(child);
        auditHelper.record(AuditAction.POLICY_RENEWAL_STARTED, actorId, "insurance_policy", child.getId(),
            toResponse(parent), toResponse(child), reason);
        return toResponse(child);
    }

    private void transition(InsurancePolicyEntity policy, PolicyStatus target, UUID actorId, String reason, AuditAction action) {
        PolicyStatus current = PolicyStatus.parse(policy.getStatus());
        current.assertCanTransitionTo(target);
        PolicyResponse old = toResponse(policy);
        writeHistory(policy);
        policy.setStatus(target.name());
        policy.setTransitionReason(reason);
        policy.setUpdatedBy(actorId);
        auditHelper.record(action, actorId, "insurance_policy", policy.getId(), old, toResponse(policy), reason);
    }

    private void writeHistory(InsurancePolicyEntity policy) {
        InsurancePolicyHistoryEntity history = new InsurancePolicyHistoryEntity();
        history.setOriginalId(policy.getId());
        history.setPolicyNumber(policy.getPolicyNumber());
        history.setFarmerId(policy.getFarmerId());
        history.setFarmId(policy.getFarmId());
        history.setPolicyTypeId(policy.getPolicyTypeId());
        history.setInsuranceCompanyId(policy.getInsuranceCompanyId());
        history.setCoverageAmount(policy.getCoverageAmount());
        history.setPremiumAmount(policy.getPremiumAmount());
        history.setCurrency(policy.getCurrency());
        history.setStartDate(policy.getStartDate());
        history.setEndDate(policy.getEndDate());
        history.setIssuedAt(policy.getIssuedAt());
        history.setStatus(policy.getStatus());
        history.setChangedAt(Instant.now());
        history.setChangedBy(policy.getUpdatedBy());
        history.setChangeReason(policy.getTransitionReason());
        historyRepository.save(history);
    }

    private void validateRefs(PolicySubmitRequest request) {
        farmerRepository.findByIdAndDeletedFalse(request.farmerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farmer not found"));
        var farm = farmRepository.findByIdAndDeletedFalse(request.farmId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm not found"));
        if (!farm.getFarmerId().equals(request.farmerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm does not belong to farmer");
        }
        var product = productRepository.findByIdAndDeletedFalse(request.productId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Product not active");
        }
        requireBoundaryIfConfigured(product.getEligibilityJson(), request.farmId());
        policyTypeRepository.findById(request.policyTypeId()).filter(t -> !t.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy type not found"));
        packageRepository.findByIdAndDeletedFalse(request.coveragePackageId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coverage package not found"));
    }

    private void requireBoundaryIfConfigured(String eligibilityJson, UUID farmId) {
        boolean required = true;
        try {
            JsonNode node = objectMapper.readTree(eligibilityJson == null ? "{}" : eligibilityJson);
            if (node.has("requireActiveBoundary")) {
                required = node.path("requireActiveBoundary").asBoolean(true);
            }
        } catch (Exception ignored) {
        }
        if (required) {
            boolean hasActive = farmBoundaryRepository.findByFarmIdAndDeletedFalse(farmId).stream()
                .anyMatch(b -> "ACTIVE".equals(b.getStatus()) && b.getGeom() != null);
            if (!hasActive) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Farm requires an ACTIVE boundary");
            }
        }
    }

    private void saveBeneficiary(UUID policyId, BeneficiaryRequest request, UUID actorId) {
        PolicyBeneficiaryEntity entity = new PolicyBeneficiaryEntity();
        entity.setPolicyId(policyId);
        entity.setFullName(request.fullName());
        entity.setRelationship(request.relationship());
        entity.setNationalId(request.nationalId());
        entity.setSharePct(request.sharePct());
        entity.setStatus("ACTIVE");
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        beneficiaryRepository.save(entity);
    }

    private String nextPolicyNumber() {
        return "POL-" + Year.now().getValue() + "-" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000));
    }

    private InsurancePolicyEntity require(UUID id) {
        return policyRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required");
        }
    }

    PolicyResponse toResponse(InsurancePolicyEntity p) {
        return new PolicyResponse(
            p.getId(), p.getPolicyNumber(), p.getFarmerId(), p.getFarmId(), p.getPolicyTypeId(),
            p.getProductId(), p.getCoveragePackageId(), p.getCropId(), p.getSeasonId(), p.getPremiumQuoteId(),
            p.getParentPolicyId(), p.getInsuranceCompanyId(), p.getCoverageAmount(), p.getPremiumAmount(),
            p.getCurrency(), p.getStartDate(), p.getEndDate(), p.getIssuedAt(), p.getStatus(), p.getTransitionReason()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
