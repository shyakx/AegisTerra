package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.application.workflow.WorkflowInstanceService;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementWorkflowLinkEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementWorkflowLinkRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionRepository;
import com.aegisterra.platform.application.contracts.SettlementCreateRequest;
import com.aegisterra.platform.application.contracts.WorkflowInstanceResponse;
import com.aegisterra.platform.application.contracts.WorkflowStartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementIntakeService {

    public static final String SOURCE_CLAIMS = "CLAIMS";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String WORKFLOW_STANDARD = "SETTLEMENT_STANDARD";

    private static final Logger log = LoggerFactory.getLogger(SettlementIntakeService.class);

    private final SettlementRepository settlementRepository;
    private final SettlementWorkflowLinkRepository linkRepository;
    private final ClaimRepository claimRepository;
    private final InsurancePolicyRepository policyRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceService workflowInstanceService;
    private final SettlementNumberGenerator numberGenerator;
    private final SettlementLifecycleService lifecycleService;
    private final SettlementAuditHelper auditHelper;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;

    public SettlementIntakeService(
        SettlementRepository settlementRepository,
        SettlementWorkflowLinkRepository linkRepository,
        ClaimRepository claimRepository,
        InsurancePolicyRepository policyRepository,
        WorkflowDefinitionRepository definitionRepository,
        WorkflowInstanceService workflowInstanceService,
        SettlementNumberGenerator numberGenerator,
        SettlementLifecycleService lifecycleService,
        SettlementAuditHelper auditHelper,
        EventBus eventBus,
        ObjectMapper objectMapper
    ) {
        this.settlementRepository = settlementRepository;
        this.linkRepository = linkRepository;
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.definitionRepository = definitionRepository;
        this.workflowInstanceService = workflowInstanceService;
        this.numberGenerator = numberGenerator;
        this.lifecycleService = lifecycleService;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<SettlementEntity> intakeFromClaimApproved(UUID claimId, UUID actorId, String correlationId) {
        Optional<SettlementEntity> existing = settlementRepository.findActiveBySource(SOURCE_CLAIMS, claimId);
        if (existing.isPresent()) {
            log.info("Settlement already exists for claim {}", claimId);
            return existing;
        }

        ClaimEntity claim = claimRepository.findByIdAndDeletedFalse(claimId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found for settlement intake"));

        BigDecimal amount = claim.getApprovedAmount() != null ? claim.getApprovedAmount()
            : (claim.getAssessedAmount() != null ? claim.getAssessedAmount() : claim.getClaimedAmount());
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Claim has no positive approved amount");
        }

        InsurancePolicyEntity policy = policyRepository.findByIdAndDeletedFalse(claim.getPolicyId()).orElse(null);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("amount", amount);
        snapshot.put("currency", claim.getCurrency());
        snapshot.put("exchangeRate", BigDecimal.ONE);
        snapshot.put("beneficiaryName", claim.getFarmerId() == null ? "UNKNOWN" : "FARMER:" + claim.getFarmerId());
        snapshot.put("beneficiaryAccount", "MANUAL");
        snapshot.put("sourceModule", SOURCE_CLAIMS);
        snapshot.put("sourceRecordId", claim.getId().toString());
        snapshot.put("sourceReference", claim.getClaimNumber());
        snapshot.put("policyId", claim.getPolicyId().toString());
        snapshot.put("farmerId", claim.getFarmerId() == null ? null : claim.getFarmerId().toString());
        snapshot.put("farmId", claim.getFarmId() == null ? null : claim.getFarmId().toString());
        snapshot.put("cropId", claim.getCropId() == null ? null : claim.getCropId().toString());
        snapshot.put("policyReference", policy == null ? null : policy.getPolicyNumber());
        snapshot.put("capturedAt", Instant.now().toString());

        return Optional.of(createAndStart(
            SOURCE_CLAIMS,
            claim.getId(),
            claim.getClaimNumber(),
            amount,
            claim.getCurrency(),
            BigDecimal.ONE,
            "MANUAL",
            "MANUAL",
            String.valueOf(snapshot.get("beneficiaryName")),
            "MANUAL",
            writeJson(snapshot),
            correlationId != null ? correlationId : claim.getCorrelationId(),
            actorId
        ));
    }

    @Transactional
    public SettlementEntity createManual(SettlementCreateRequest request, UUID actorId) {
        String source = request.sourceModule() == null ? SOURCE_MANUAL : request.sourceModule().trim().toUpperCase();
        Optional<SettlementEntity> existing = settlementRepository.findActiveBySource(source, request.sourceRecordId());
        if (existing.isPresent()) {
            return existing.get();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (request.financialSnapshotJson() == null || request.financialSnapshotJson().isBlank()) {
            snapshot.put("amount", request.amount());
            snapshot.put("currency", request.currency());
            snapshot.put("exchangeRate", request.exchangeRate() == null ? BigDecimal.ONE : request.exchangeRate());
            snapshot.put("beneficiaryName", request.beneficiaryName());
            snapshot.put("beneficiaryAccount", request.beneficiaryAccount());
            snapshot.put("sourceModule", source);
            snapshot.put("sourceRecordId", request.sourceRecordId().toString());
            snapshot.put("sourceReference", request.sourceReference());
            snapshot.put("capturedAt", Instant.now().toString());
        }
        return createAndStart(
            source,
            request.sourceRecordId(),
            request.sourceReference(),
            request.amount(),
            request.currency().trim().toUpperCase(),
            request.exchangeRate() == null ? BigDecimal.ONE : request.exchangeRate(),
            blankOr(request.paymentMethod(), "MANUAL").toUpperCase(),
            blankOr(request.providerCode(), "MANUAL").toUpperCase(),
            request.beneficiaryName(),
            request.beneficiaryAccount(),
            request.financialSnapshotJson() == null || request.financialSnapshotJson().isBlank()
                ? writeJson(snapshot) : request.financialSnapshotJson(),
            request.correlationId(),
            actorId
        );
    }

    private SettlementEntity createAndStart(
        String sourceModule,
        UUID sourceRecordId,
        String sourceReference,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        String paymentMethod,
        String providerCode,
        String beneficiaryName,
        String beneficiaryAccount,
        String snapshotJson,
        String correlationId,
        UUID actorId
    ) {
        SettlementEntity settlement = new SettlementEntity();
        settlement.setSettlementNumber(numberGenerator.next());
        settlement.setSourceModule(sourceModule);
        settlement.setSourceRecordId(sourceRecordId);
        settlement.setSourceReference(sourceReference);
        settlement.setAmount(amount);
        settlement.setCurrency(currency);
        settlement.setExchangeRate(exchangeRate);
        settlement.setPaymentMethod(paymentMethod);
        settlement.setProviderCode(providerCode);
        settlement.setBeneficiaryName(beneficiaryName);
        settlement.setBeneficiaryAccount(beneficiaryAccount);
        settlement.setFinancialSnapshotJson(snapshotJson);
        settlement.setCorrelationId(correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString() : correlationId);
        settlement.setSubmittedAt(Instant.now());
        settlement.setDeleted(false);
        settlement.setCreatedBy(actorId);
        settlement.setStatus(SettlementStatus.PENDING.name());
        settlementRepository.save(settlement);

        lifecycleService.transition(settlement, SettlementStatus.UNDER_REVIEW, actorId, "intake");

        WorkflowDefinitionEntity definition = definitionRepository.findByCodeAndDeletedFalse(WORKFLOW_STANDARD)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Workflow definition not found: " + WORKFLOW_STANDARD));

        WorkflowInstanceResponse instance = workflowInstanceService.start(
            new WorkflowStartRequest(
                definition.getId(),
                "SETTLEMENT",
                settlement.getId(),
                settlement.getCorrelationId(),
                "{\"settlementNumber\":\"" + settlement.getSettlementNumber() + "\"}"
            ),
            actorId
        );
        settlement.setWorkflowInstanceId(instance.id());
        settlement.setWorkflowDefinitionCode(WORKFLOW_STANDARD);
        settlementRepository.save(settlement);

        SettlementWorkflowLinkEntity link = new SettlementWorkflowLinkEntity();
        link.setSettlementId(settlement.getId());
        link.setWorkflowInstanceId(instance.id());
        link.setWorkflowDefinitionCode(WORKFLOW_STANDARD);
        link.setLinkRole("PRIMARY");
        link.setDeleted(false);
        link.setCreatedBy(actorId);
        link.setStatus("ACTIVE");
        linkRepository.save(link);

        auditHelper.record(AuditAction.SETTLEMENT_CREATED, actorId, "settlement", settlement.getId(),
            null, settlement.getSettlementNumber(), sourceModule);

        Map<String, Object> payload = new HashMap<>();
        payload.put("settlementNumber", settlement.getSettlementNumber());
        payload.put("amount", amount.toPlainString());
        payload.put("currency", currency);
        payload.put("sourceModule", sourceModule);
        payload.put("sourceRecordId", sourceRecordId.toString());
        payload.put("workflowInstanceId", instance.id().toString());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.SETTLEMENT_CREATED,
            actorId,
            "SETTLEMENT",
            settlement.getId(),
            settlement.getCorrelationId(),
            payload
        ));
        return settlement;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
