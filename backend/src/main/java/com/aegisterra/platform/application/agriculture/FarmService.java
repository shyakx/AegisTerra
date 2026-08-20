package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.application.contracts.FarmRequest;
import com.aegisterra.platform.application.contracts.FarmResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FarmService {

    private final FarmRepository farmRepository;
    private final FarmerRepository farmerRepository;
    private final AgricultureAuditHelper auditHelper;

    public FarmService(
        FarmRepository farmRepository,
        FarmerRepository farmerRepository,
        AgricultureAuditHelper auditHelper
    ) {
        this.farmRepository = farmRepository;
        this.farmerRepository = farmerRepository;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<FarmResponse> search(
        String q,
        UUID farmerId,
        UUID districtId,
        UUID villageId,
        String status,
        Pageable pageable
    ) {
        return PageResponse.from(farmRepository.search(
            blankToNull(q), farmerId, districtId, villageId, blankToNull(status), pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public FarmResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(String q, UUID farmerId, UUID districtId, UUID villageId, String status) {
        var page = farmRepository.search(
            blankToNull(q), farmerId, districtId, villageId, blankToNull(status), PageRequest.of(0, 10_000)
        );
        return page.getContent().stream()
            .map(f -> String.join(",",
                csv(f.getId().toString()),
                csv(f.getFarmCode()),
                csv(f.getFarmName()),
                csv(f.getFarmerId().toString()),
                csv(f.getFarmSizeHa() == null ? "" : f.getFarmSizeHa().toPlainString()),
                csv(f.getStatus())
            ))
            .collect(Collectors.joining("\n", "id,farmCode,farmName,farmerId,farmSizeHa,status\n", "\n"));
    }

    @Transactional
    public FarmResponse create(FarmRequest request, UUID actorId) {
        farmerRepository.findByIdAndDeletedFalse(request.farmerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farmer not found"));
        if (farmRepository.existsByFarmerIdAndFarmNameIgnoreCaseAndDeletedFalse(request.farmerId(), request.farmName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Farm name already exists for this farmer");
        }
        String code = request.farmCode() == null || request.farmCode().isBlank()
            ? GeometryService.generateCode("FARM")
            : request.farmCode().trim();
        farmRepository.findByFarmCodeAndDeletedFalse(code).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Farm code already exists");
        });
        FarmEntity farm = FarmEntity.create(request.farmerId(), code, request.farmName().trim());
        farm.setFarmSizeHa(request.farmSizeHa());
        farm.setCropType(request.cropType());
        farm.setDistrictId(request.districtId());
        farm.setSectorId(request.sectorId());
        farm.setCellId(request.cellId());
        farm.setVillageId(request.villageId());
        farm.setCreatedBy(actorId);
        if (request.status() != null && !request.status().isBlank()) {
            farm.setStatus(request.status().trim().toUpperCase());
        }
        farmRepository.save(farm);
        auditHelper.record(AuditAction.FARM_CREATED, actorId, "farm", farm.getId(), null, toResponse(farm), request.reason());
        return toResponse(farm);
    }

    @Transactional
    public FarmResponse update(UUID id, FarmRequest request, UUID actorId) {
        FarmEntity farm = require(id);
        FarmResponse old = toResponse(farm);
        if (farmRepository.existsByFarmerIdAndFarmNameIgnoreCaseAndDeletedFalseAndIdNot(
            farm.getFarmerId(), request.farmName().trim(), id
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Farm name already exists for this farmer");
        }
        farm.setFarmName(request.farmName().trim());
        if (request.farmCode() != null && !request.farmCode().isBlank()
            && !request.farmCode().equals(farm.getFarmCode())) {
            farmRepository.findByFarmCodeAndDeletedFalse(request.farmCode().trim()).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Farm code already exists");
            });
            farm.setFarmCode(request.farmCode().trim());
        }
        farm.setFarmSizeHa(request.farmSizeHa());
        farm.setCropType(request.cropType());
        farm.setDistrictId(request.districtId());
        farm.setSectorId(request.sectorId());
        farm.setCellId(request.cellId());
        farm.setVillageId(request.villageId());
        farm.setUpdatedBy(actorId);
        if (request.status() != null && !request.status().isBlank()) {
            farm.setStatus(request.status().trim().toUpperCase());
            auditHelper.record(AuditAction.FARM_STATUS_CHANGED, actorId, "farm", id, old, toResponse(farm), request.reason());
        } else {
            auditHelper.record(AuditAction.FARM_UPDATED, actorId, "farm", id, old, toResponse(farm), request.reason());
        }
        return toResponse(farm);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        FarmEntity farm = require(id);
        FarmResponse old = toResponse(farm);
        farm.setDeleted(true);
        farm.setStatus("DELETED");
        farm.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.FARM_UPDATED, actorId, "farm", id, old, toResponse(farm), "soft-delete");
    }

    private FarmEntity require(UUID id) {
        return farmRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
    }

    FarmResponse toResponse(FarmEntity farm) {
        return new FarmResponse(
            farm.getId(),
            farm.getFarmerId(),
            farm.getFarmCode(),
            farm.getFarmName(),
            farm.getFarmSizeHa(),
            farm.getCropType(),
            farm.getDistrictId(),
            farm.getSectorId(),
            farm.getCellId(),
            farm.getVillageId(),
            farm.getStatus()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
