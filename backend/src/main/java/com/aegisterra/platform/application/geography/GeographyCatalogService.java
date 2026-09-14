package com.aegisterra.platform.application.geography;

import com.aegisterra.platform.application.contracts.AgroecologicalSubzoneResponse;
import com.aegisterra.platform.application.contracts.AgroecologicalZoneResponse;
import com.aegisterra.platform.application.contracts.DistrictResponse;
import com.aegisterra.platform.application.contracts.ProvinceResponse;
import com.aegisterra.platform.infrastructure.persistence.geography.AgroecologicalSubzoneEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.AgroecologicalSubzoneRepository;
import com.aegisterra.platform.infrastructure.persistence.geography.AgroecologicalZoneEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.AgroecologicalZoneRepository;
import com.aegisterra.platform.infrastructure.persistence.geography.DistrictEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.DistrictRepository;
import com.aegisterra.platform.infrastructure.persistence.geography.ProvinceEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.ProvinceRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only catalog for administrative geography and agroecological classification (ADR-009).
 * Reference data is migration-controlled; this service does not mutate it.
 */
@Service
public class GeographyCatalogService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final AgroecologicalZoneRepository zoneRepository;
    private final AgroecologicalSubzoneRepository subzoneRepository;

    public GeographyCatalogService(
        ProvinceRepository provinceRepository,
        DistrictRepository districtRepository,
        AgroecologicalZoneRepository zoneRepository,
        AgroecologicalSubzoneRepository subzoneRepository
    ) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.zoneRepository = zoneRepository;
        this.subzoneRepository = subzoneRepository;
    }

    @Transactional(readOnly = true)
    public List<ProvinceResponse> listProvinces() {
        return provinceRepository.findByDeletedFalseOrderByNameAsc().stream()
            .map(this::toProvince)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProvinceResponse getProvince(UUID id) {
        return toProvince(requireProvince(id));
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> listDistricts(UUID provinceId) {
        Catalog catalog = loadCatalog();
        List<DistrictEntity> districts = provinceId == null
            ? districtRepository.findByDeletedFalseOrderByNameAsc()
            : districtRepository.findByProvinceIdAndDeletedFalseOrderByNameAsc(provinceId);
        return districts.stream().map(district -> toDistrict(district, catalog)).toList();
    }

    @Transactional(readOnly = true)
    public DistrictResponse getDistrict(UUID id) {
        return toDistrict(requireDistrict(id), loadCatalog());
    }

    @Transactional(readOnly = true)
    public List<AgroecologicalZoneResponse> listZones() {
        return zoneRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .map(this::toZone)
            .toList();
    }

    @Transactional(readOnly = true)
    public AgroecologicalZoneResponse getZone(UUID id) {
        return toZone(requireZone(id));
    }

    @Transactional(readOnly = true)
    public List<AgroecologicalSubzoneResponse> listSubzones(UUID zoneId, UUID districtId) {
        Catalog catalog = loadCatalog();
        List<AgroecologicalSubzoneEntity> subzones;
        if (districtId != null) {
            DistrictEntity district = requireDistrict(districtId);
            if (district.getAgroecologicalSubzoneId() == null) {
                return List.of();
            }
            AgroecologicalSubzoneEntity subzone = catalog.subzones.get(district.getAgroecologicalSubzoneId());
            if (subzone == null || subzone.isDeleted()) {
                return List.of();
            }
            if (zoneId != null && !zoneId.equals(subzone.getZoneId())) {
                return List.of();
            }
            subzones = List.of(subzone);
        } else if (zoneId != null) {
            subzones = subzoneRepository.findByZoneIdAndDeletedFalseOrderByCodeAsc(zoneId);
        } else {
            subzones = subzoneRepository.findByDeletedFalseOrderByCodeAsc();
        }
        return subzones.stream().map(subzone -> toSubzone(subzone, catalog)).toList();
    }

    @Transactional(readOnly = true)
    public AgroecologicalSubzoneResponse getSubzone(UUID id) {
        return toSubzone(requireSubzone(id), loadCatalog());
    }

    private ProvinceEntity requireProvince(UUID id) {
        return provinceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Province not found"));
    }

    private DistrictEntity requireDistrict(UUID id) {
        return districtRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "District not found"));
    }

    private AgroecologicalZoneEntity requireZone(UUID id) {
        return zoneRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agroecological zone not found"));
    }

    private AgroecologicalSubzoneEntity requireSubzone(UUID id) {
        return subzoneRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agroecological sub-zone not found"));
    }

    private Catalog loadCatalog() {
        Map<UUID, ProvinceEntity> provinces = provinceRepository.findByDeletedFalseOrderByNameAsc().stream()
            .collect(Collectors.toMap(ProvinceEntity::getId, Function.identity()));
        Map<UUID, AgroecologicalZoneEntity> zones = zoneRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .collect(Collectors.toMap(AgroecologicalZoneEntity::getId, Function.identity()));
        Map<UUID, AgroecologicalSubzoneEntity> subzones = subzoneRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .collect(Collectors.toMap(AgroecologicalSubzoneEntity::getId, Function.identity()));
        return new Catalog(provinces, zones, subzones);
    }

    private ProvinceResponse toProvince(ProvinceEntity province) {
        return new ProvinceResponse(province.getId(), province.getCode(), province.getName(), province.getStatus());
    }

    private AgroecologicalZoneResponse toZone(AgroecologicalZoneEntity zone) {
        return new AgroecologicalZoneResponse(zone.getId(), zone.getCode(), zone.getName(), zone.getStatus());
    }

    private AgroecologicalSubzoneResponse toSubzone(AgroecologicalSubzoneEntity subzone, Catalog catalog) {
        AgroecologicalZoneEntity zone = catalog.zones.get(subzone.getZoneId());
        return new AgroecologicalSubzoneResponse(
            subzone.getId(),
            subzone.getCode(),
            subzone.getName(),
            subzone.getStatus(),
            subzone.getZoneId(),
            zone == null ? null : zone.getCode(),
            zone == null ? null : zone.getName()
        );
    }

    private DistrictResponse toDistrict(DistrictEntity district, Catalog catalog) {
        ProvinceEntity province = district.getProvinceId() == null ? null : catalog.provinces.get(district.getProvinceId());
        AgroecologicalSubzoneEntity subzone = district.getAgroecologicalSubzoneId() == null
            ? null
            : catalog.subzones.get(district.getAgroecologicalSubzoneId());
        AgroecologicalZoneEntity zone = subzone == null ? null : catalog.zones.get(subzone.getZoneId());
        return new DistrictResponse(
            district.getId(),
            district.getCode(),
            district.getName(),
            district.getStatus(),
            district.getProvinceId(),
            province == null ? null : province.getCode(),
            province == null ? null : province.getName(),
            district.getAgroecologicalSubzoneId(),
            subzone == null ? null : subzone.getCode(),
            subzone == null ? null : subzone.getName(),
            zone == null ? null : zone.getId(),
            zone == null ? null : zone.getCode(),
            zone == null ? null : zone.getName()
        );
    }

    private record Catalog(
        Map<UUID, ProvinceEntity> provinces,
        Map<UUID, AgroecologicalZoneEntity> zones,
        Map<UUID, AgroecologicalSubzoneEntity> subzones
    ) {}
}
