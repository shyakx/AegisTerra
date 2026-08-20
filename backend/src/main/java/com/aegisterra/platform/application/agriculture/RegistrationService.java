package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.RegistrationDraftEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.RegistrationDraftRepository;
import com.aegisterra.platform.application.contracts.CropSeasonRequest;
import com.aegisterra.platform.application.contracts.FarmBoundaryRequest;
import com.aegisterra.platform.application.contracts.FarmRequest;
import com.aegisterra.platform.application.contracts.FarmerRequest;
import com.aegisterra.platform.application.contracts.HouseholdRequest;
import com.aegisterra.platform.application.contracts.PlotRequest;
import com.aegisterra.platform.application.contracts.RegistrationDraftRequest;
import com.aegisterra.platform.application.contracts.RegistrationDraftResponse;
import com.aegisterra.platform.application.contracts.RegistrationSubmitResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

    private final RegistrationDraftRepository draftRepository;
    private final GeometryService geometryService;
    private final HouseholdService householdService;
    private final FarmerService farmerService;
    private final FarmService farmService;
    private final BoundaryService boundaryService;
    private final PlotService plotService;
    private final CropSeasonService cropSeasonService;
    private final AgricultureAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public RegistrationService(
        RegistrationDraftRepository draftRepository,
        GeometryService geometryService,
        HouseholdService householdService,
        FarmerService farmerService,
        FarmService farmService,
        BoundaryService boundaryService,
        PlotService plotService,
        CropSeasonService cropSeasonService,
        AgricultureAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.draftRepository = draftRepository;
        this.geometryService = geometryService;
        this.householdService = householdService;
        this.farmerService = farmerService;
        this.farmService = farmService;
        this.boundaryService = boundaryService;
        this.plotService = plotService;
        this.cropSeasonService = cropSeasonService;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDraftResponse> listMine(UUID userId) {
        return draftRepository.findByCreatedByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public RegistrationDraftResponse get(UUID id, UUID userId) {
        return toResponse(requireOwned(id, userId));
    }

    @Transactional
    public RegistrationDraftResponse create(RegistrationDraftRequest request, UUID userId) {
        Instant expires = Instant.now().plus(geometryService.draftExpiryDays(), ChronoUnit.DAYS);
        RegistrationDraftEntity draft = RegistrationDraftEntity.create(
            userId,
            request.payloadJson(),
            expires
        );
        draft.setCurrentStep(request.currentStep());
        draftRepository.save(draft);
        auditHelper.record(AuditAction.REGISTRATION_DRAFT_SAVED, userId, "registration_draft", draft.getId(),
            null, toResponse(draft), null);
        return toResponse(draft);
    }

    @Transactional
    public RegistrationDraftResponse update(UUID id, RegistrationDraftRequest request, UUID userId) {
        RegistrationDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft is not editable");
        }
        if (draft.getExpiresAt() != null && draft.getExpiresAt().isBefore(Instant.now())) {
            draft.setStatus("EXPIRED");
            throw new ResponseStatusException(HttpStatus.GONE, "Draft has expired");
        }
        RegistrationDraftResponse old = toResponse(draft);
        draft.setCurrentStep(request.currentStep());
        draft.setPayloadJson(request.payloadJson());
        draft.setUpdatedBy(userId);
        auditHelper.record(AuditAction.REGISTRATION_DRAFT_SAVED, userId, "registration_draft", id, old, toResponse(draft), null);
        return toResponse(draft);
    }

    @Transactional
    public RegistrationSubmitResponse submit(UUID id, UUID userId) {
        RegistrationDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft cannot be submitted");
        }
        if (draft.getExpiresAt() != null && draft.getExpiresAt().isBefore(Instant.now())) {
            draft.setStatus("EXPIRED");
            throw new ResponseStatusException(HttpStatus.GONE, "Draft has expired");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(draft.getPayloadJson());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft payload is not valid JSON");
        }

        UUID householdId = null;
        JsonNode household = payload.path("household");
        boolean skipHousehold = household.path("skip").asBoolean(false);
        if (!skipHousehold && !household.isMissingNode() && !household.isNull()
            && household.path("code").asText(null) != null) {
            householdId = householdService.create(new HouseholdRequest(
                household.path("code").asText(),
                household.path("headName").asText(null)
            ), userId).id();
        } else if (!skipHousehold && household.hasNonNull("id")) {
            householdId = UUID.fromString(household.path("id").asText());
        }

        JsonNode farmerNode = payload.path("farmer");
        requireNode(farmerNode, "farmer");
        var farmer = farmerService.create(new FarmerRequest(
            householdId,
            farmerNode.path("firstName").asText(),
            farmerNode.path("lastName").asText(),
            farmerNode.path("nationalId").asText(),
            farmerNode.path("phoneNumber").asText(),
            textOrNull(farmerNode, "email"),
            uuidOrNull(farmerNode, "districtId"),
            uuidOrNull(farmerNode, "sectorId"),
            uuidOrNull(farmerNode, "cellId"),
            uuidOrNull(farmerNode, "villageId"),
            "PENDING_VERIFICATION",
            "registration-submit"
        ), userId);

        JsonNode farmNode = payload.path("farm");
        requireNode(farmNode, "farm");
        var farm = farmService.create(new FarmRequest(
            farmer.id(),
            textOrNull(farmNode, "farmCode"),
            farmNode.path("farmName").asText(),
            decimalOrNull(farmNode, "farmSizeHa"),
            textOrNull(farmNode, "cropType"),
            uuidOrNull(farmNode, "districtId"),
            uuidOrNull(farmNode, "sectorId"),
            uuidOrNull(farmNode, "cellId"),
            uuidOrNull(farmNode, "villageId"),
            "DRAFT",
            "registration-submit"
        ), userId);

        JsonNode boundaryNode = payload.path("boundary");
        boolean skipBoundary = boundaryNode.path("skip").asBoolean(false);
        String geoJson = boundaryNode.path("geoJson").asText(null);
        if (!skipBoundary && (geoJson == null || geoJson.isBlank())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Farm boundary is required (or set boundary.skip=true)");
        }
        if (!skipBoundary) {
            boundaryService.create(new FarmBoundaryRequest(
                farm.id(),
                geoJson,
                "MAPLIBRE",
                "ACTIVE",
                "registration-submit"
            ), userId);
        }

        JsonNode plots = payload.path("plots");
        if (plots.isArray()) {
            for (JsonNode plot : plots) {
                plotService.create(new PlotRequest(
                    farm.id(),
                    plot.path("plotCode").asText(),
                    textOrNull(plot, "name"),
                    textOrNull(plot, "geoJson"),
                    decimalOrNull(plot, "areaHa"),
                    "ACTIVE",
                    "registration-submit"
                ), userId);
            }
        }

        JsonNode cropSeasons = payload.path("cropSeasons");
        if (cropSeasons.isArray()) {
            for (JsonNode cs : cropSeasons) {
                cropSeasonService.create(new CropSeasonRequest(
                    farm.id(),
                    uuidOrNull(cs, "plotId"),
                    UUID.fromString(cs.path("cropId").asText()),
                    UUID.fromString(cs.path("seasonId").asText()),
                    decimalOrNull(cs, "plantedAreaHa"),
                    "PLANNED",
                    "registration-submit"
                ), userId);
            }
        }

        draft.setFarmerId(farmer.id());
        draft.setStatus("SUBMITTED");
        draft.setUpdatedBy(userId);
        auditHelper.record(AuditAction.REGISTRATION_SUBMITTED, userId, "registration_draft", draft.getId(),
            null, toResponse(draft), null);

        return new RegistrationSubmitResponse(draft.getId(), farmer.id(), farm.id(), "SUBMITTED");
    }

    private RegistrationDraftEntity requireOwned(UUID id, UUID userId) {
        RegistrationDraftEntity draft = draftRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!draft.getCreatedByUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Draft not owned by current user");
        }
        return draft;
    }

    private RegistrationDraftResponse toResponse(RegistrationDraftEntity draft) {
        return new RegistrationDraftResponse(
            draft.getId(),
            draft.getCreatedByUserId(),
            draft.getFarmerId(),
            draft.getCurrentStep(),
            draft.getPayloadJson(),
            draft.getStatus(),
            draft.getExpiresAt(),
            draft.getUpdatedAt()
        );
    }

    private static void requireNode(JsonNode node, String name) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, name + " section is required");
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private static UUID uuidOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        return text == null ? null : UUID.fromString(text);
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }
}
