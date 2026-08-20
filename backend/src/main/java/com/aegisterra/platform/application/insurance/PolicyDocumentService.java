package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyDocumentEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyDocumentRepository;
import com.aegisterra.platform.infrastructure.persistence.platform.ConfigurationRepository;
import com.aegisterra.platform.application.contracts.PolicyDocumentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyDocumentService {

    private static final List<String> TYPES = List.of(
        "POLICY_CERTIFICATE", "TERMS_AND_CONDITIONS", "COVERAGE_SUMMARY"
    );

    private final PolicyDocumentRepository documentRepository;
    private final ConfigurationRepository configurationRepository;
    private final InsuranceProductRepository productRepository;
    private final InsuranceAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public PolicyDocumentService(
        PolicyDocumentRepository documentRepository,
        ConfigurationRepository configurationRepository,
        InsuranceProductRepository productRepository,
        InsuranceAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.configurationRepository = configurationRepository;
        this.productRepository = productRepository;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PolicyDocumentResponse> list(UUID policyId) {
        return documentRepository.findByPolicyIdAndDeletedFalseOrderByDocumentTypeAscVersionNoDesc(policyId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void generateAll(InsurancePolicyEntity policy, UUID actorId) {
        for (String type : TYPES) {
            generate(policy, type, actorId);
        }
    }

    @Transactional
    public PolicyDocumentResponse regenerate(InsurancePolicyEntity policy, String documentType, UUID actorId) {
        return toResponse(generate(policy, documentType, actorId));
    }

    private PolicyDocumentEntity generate(InsurancePolicyEntity policy, String documentType, UUID actorId) {
        int nextVersion = documentRepository
            .findFirstByPolicyIdAndDocumentTypeAndDeletedFalseOrderByVersionNoDesc(policy.getId(), documentType)
            .map(d -> d.getVersionNo() + 1)
            .orElse(1);

        JsonNode template = loadTemplate(documentType);
        String templateCode = template.path("templateCode").asText(documentType + "_V1");
        String body = template.path("body").asText("Document {{policyNumber}}");

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("policyNumber", policy.getPolicyNumber());
        vars.put("farmerId", policy.getFarmerId().toString());
        vars.put("farmId", policy.getFarmId().toString());
        vars.put("coverageAmount", policy.getCoverageAmount().toPlainString());
        vars.put("premiumAmount", policy.getPremiumAmount().toPlainString());
        vars.put("currency", policy.getCurrency());
        vars.put("startDate", policy.getStartDate().toString());
        vars.put("endDate", policy.getEndDate().toString());
        vars.put("coverageSnapshot", policy.getCoverageSnapshotJson() == null ? "{}" : policy.getCoverageSnapshotJson());
        String productCode = productRepository.findById(policy.getProductId()).map(InsuranceProductEntity::getCode).orElse("");
        vars.put("productCode", productCode);
        vars.put("packageCode", policy.getCoveragePackageId() == null ? "" : policy.getCoveragePackageId().toString());

        String provisional = render(body, vars);
        String sha = sha256(provisional);
        String qr = policy.getPolicyNumber() + "|" + nextVersion + "|" + sha;
        vars.put("qrPayload", qr);
        vars.put("contentSha256", sha);
        String content = render(body, vars);
        content = content.replace("{{qrPayload}}", qr).replace("{{contentSha256}}", sha);
        sha = sha256(content);
        qr = policy.getPolicyNumber() + "|" + nextVersion + "|" + sha;

        PolicyDocumentEntity doc = new PolicyDocumentEntity();
        doc.setPolicyId(policy.getId());
        doc.setDocumentType(documentType);
        doc.setVersionNo(nextVersion);
        doc.setTemplateCode(templateCode);
        doc.setContentText(content);
        doc.setContentSha256(sha);
        doc.setQrPayload(qr);
        doc.setSignatureStatus("PENDING");
        doc.setStatus("ACTIVE");
        doc.setDeleted(false);
        doc.setCreatedBy(actorId);
        documentRepository.save(doc);
        auditHelper.record(AuditAction.POLICY_DOCUMENT_GENERATED, actorId, "policy_document", doc.getId(),
            null, toResponse(doc), documentType);
        return doc;
    }

    private JsonNode loadTemplate(String documentType) {
        String key = "insurance.doc.template." + documentType;
        return configurationRepository.findByConfigKeyAndDeletedFalse(key)
            .map(cfg -> {
                try {
                    return objectMapper.readTree(cfg.getValueJson());
                } catch (Exception ex) {
                    return objectMapper.createObjectNode();
                }
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing document template: " + key));
    }

    private static String render(String body, Map<String, String> vars) {
        String result = body;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private PolicyDocumentResponse toResponse(PolicyDocumentEntity doc) {
        return new PolicyDocumentResponse(
            doc.getId(), doc.getPolicyId(), doc.getDocumentType(), doc.getVersionNo(), doc.getTemplateCode(),
            doc.getContentText(), doc.getContentSha256(), doc.getQrPayload(), doc.getSignatureStatus(),
            doc.getSignedAt(), doc.getStatus()
        );
    }
}
