package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmBoundaryRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.HouseholdRepository;
import com.aegisterra.platform.application.contracts.FarmerRequest;
import com.aegisterra.platform.application.contracts.FarmerResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FarmerService {

    private static final Set<String> ALLOWED_STATUS = Set.of(
        "PENDING_VERIFICATION", "ACTIVE", "SUSPENDED", "INACTIVE"
    );

    private final FarmerRepository farmerRepository;
    private final HouseholdRepository householdRepository;
    private final FarmRepository farmRepository;
    private final FarmBoundaryRepository farmBoundaryRepository;
    private final AgricultureAuditHelper auditHelper;

    public FarmerService(
        FarmerRepository farmerRepository,
        HouseholdRepository householdRepository,
        FarmRepository farmRepository,
        FarmBoundaryRepository farmBoundaryRepository,
        AgricultureAuditHelper auditHelper
    ) {
        this.farmerRepository = farmerRepository;
        this.householdRepository = householdRepository;
        this.farmRepository = farmRepository;
        this.farmBoundaryRepository = farmBoundaryRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<FarmerResponse> search(
        String q,
        String nationalId,
        String phone,
        String farmerCode,
        UUID householdId,
        UUID districtId,
        UUID villageId,
        String status,
        Pageable pageable
    ) {
        return PageResponse.from(farmerRepository.search(
            blankToNull(q),
            blankToNull(nationalId),
            blankToNull(phone),
            blankToNull(farmerCode),
            householdId,
            districtId,
            villageId,
            blankToNull(status),
            pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public FarmerResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(
        String q,
        String nationalId,
        String phone,
        String farmerCode,
        UUID householdId,
        UUID districtId,
        UUID villageId,
        String status
    ) {
        var page = farmerRepository.search(
            blankToNull(q), blankToNull(nationalId), blankToNull(phone), blankToNull(farmerCode),
            householdId, districtId, villageId, blankToNull(status),
            org.springframework.data.domain.PageRequest.of(0, 10_000)
        );
        return page.getContent().stream()
            .map(f -> String.join(",",
                csv(f.getId().toString()),
                csv(f.getFarmerCode()),
                csv(f.getFirstName()),
                csv(f.getLastName()),
                csv(f.getNationalId()),
                csv(f.getPhoneNumber()),
                csv(f.getEmail()),
                csv(f.getStatus())
            ))
            .collect(Collectors.joining("\n",
                "id,farmerCode,firstName,lastName,nationalId,phoneNumber,email,status\n",
                "\n"));
    }

    @Transactional
    public FarmerResponse create(FarmerRequest request, UUID actorId) {
        validateIdentity(request, null);
        if (request.householdId() != null) {
            householdRepository.findByIdAndDeletedFalse(request.householdId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Household not found"));
        }
        FarmerEntity farmer = FarmerEntity.create(
            request.firstName().trim(),
            request.lastName().trim(),
            request.nationalId().trim(),
            GeometryService.normalizePhone(request.phoneNumber())
        );
        farmer.setFarmerCode(GeometryService.generateCode("FRM"));
        farmer.setEmail(blankToNull(request.email()));
        farmer.setHouseholdId(request.householdId());
        farmer.setDistrictId(request.districtId());
        farmer.setSectorId(request.sectorId());
        farmer.setCellId(request.cellId());
        farmer.setVillageId(request.villageId());
        farmer.setCreatedBy(actorId);
        if (request.status() != null && !request.status().isBlank()) {
            applyStatus(farmer, request.status(), false);
        }
        farmerRepository.save(farmer);
        auditHelper.record(AuditAction.FARMER_CREATED, actorId, "farmer", farmer.getId(), null, toResponse(farmer), request.reason());
        return toResponse(farmer);
    }

    @Transactional
    public FarmerResponse update(UUID id, FarmerRequest request, UUID actorId) {
        FarmerEntity farmer = require(id);
        FarmerResponse old = toResponse(farmer);
        validateIdentity(request, id);
        farmer.setFirstName(request.firstName().trim());
        farmer.setLastName(request.lastName().trim());
        farmer.setNationalId(request.nationalId().trim());
        farmer.setPhoneNumber(GeometryService.normalizePhone(request.phoneNumber()));
        farmer.setEmail(blankToNull(request.email()));
        farmer.setHouseholdId(request.householdId());
        farmer.setDistrictId(request.districtId());
        farmer.setSectorId(request.sectorId());
        farmer.setCellId(request.cellId());
        farmer.setVillageId(request.villageId());
        farmer.setUpdatedBy(actorId);
        if (request.status() != null && !request.status().isBlank()) {
            applyStatus(farmer, request.status(), true);
            auditHelper.record(AuditAction.FARMER_STATUS_CHANGED, actorId, "farmer", id, old, toResponse(farmer), request.reason());
        } else {
            auditHelper.record(AuditAction.FARMER_UPDATED, actorId, "farmer", id, old, toResponse(farmer), request.reason());
        }
        return toResponse(farmer);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        FarmerEntity farmer = require(id);
        FarmerResponse old = toResponse(farmer);
        farmer.setDeleted(true);
        farmer.setStatus("DELETED");
        farmer.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.FARMER_UPDATED, actorId, "farmer", id, old, toResponse(farmer), "soft-delete");
    }

    private void applyStatus(FarmerEntity farmer, String status, boolean enforceActivationRules) {
        String next = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(next)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid farmer status");
        }
        if ("ACTIVE".equals(next) && enforceActivationRules) {
            boolean hasActiveBoundary = farmRepository.findByFarmerIdAndDeletedFalse(farmer.getId()).stream()
                .anyMatch(farm -> farmBoundaryRepository.findByFarmIdAndDeletedFalse(farm.getId()).stream()
                    .anyMatch(b -> "ACTIVE".equals(b.getStatus()) && b.getGeom() != null));
            if (!hasActiveBoundary) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot activate farmer without an ACTIVE farm boundary");
            }
        }
        farmer.setStatus(next);
    }

    private void validateIdentity(FarmerRequest request, UUID excludeId) {
        GeometryService.requireNationalId(request.nationalId());
        GeometryService.requirePhone(request.phoneNumber());
        farmerRepository.findByNationalIdAndDeletedFalse(request.nationalId().trim())
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "National ID already exists");
            });
        farmerRepository.findByPhoneNumberAndDeletedFalse(GeometryService.normalizePhone(request.phoneNumber()))
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
            });
        if (request.email() != null && !request.email().isBlank()) {
            farmerRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email().trim())
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });
        }
    }

    private FarmerEntity require(UUID id) {
        return farmerRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farmer not found"));
    }

    FarmerResponse toResponse(FarmerEntity farmer) {
        return new FarmerResponse(
            farmer.getId(),
            farmer.getFarmerCode(),
            farmer.getHouseholdId(),
            farmer.getFirstName(),
            farmer.getLastName(),
            farmer.getNationalId(),
            farmer.getPhoneNumber(),
            farmer.getEmail(),
            farmer.getDistrictId(),
            farmer.getSectorId(),
            farmer.getCellId(),
            farmer.getVillageId(),
            farmer.getStatus()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
