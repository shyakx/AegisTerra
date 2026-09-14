package com.aegisterra.platform.application.geography;

import com.aegisterra.platform.infrastructure.persistence.geography.DistrictEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.DistrictRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application-level catalog checks for administrative geography.
 * Null district remains valid (legacy APIs and demo farms). Unknown or soft-deleted
 * districts are rejected so arbitrary UUIDs cannot persist.
 */
@Component
public class GeographyReferenceValidator {

    private final DistrictRepository districtRepository;

    public GeographyReferenceValidator(DistrictRepository districtRepository) {
        this.districtRepository = districtRepository;
    }

    public void requireActiveDistrict(UUID districtId) {
        requireActiveDistrict(districtId, null);
    }

    public void requireActiveDistrict(UUID districtId, UUID provinceId) {
        if (districtId == null) {
            return;
        }
        DistrictEntity district = districtRepository.findByIdAndDeletedFalse(districtId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive district"));
        if (provinceId != null && !provinceId.equals(district.getProvinceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "District does not belong to the selected province");
        }
    }
}
