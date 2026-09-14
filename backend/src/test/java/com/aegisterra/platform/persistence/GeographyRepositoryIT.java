package com.aegisterra.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.persistence.geography.DistrictEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.DistrictRepository;
import com.aegisterra.platform.infrastructure.persistence.geography.ProvinceEntity;
import com.aegisterra.platform.infrastructure.persistence.geography.ProvinceRepository;
import com.aegisterra.platform.support.SharedPostgresContainer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class GeographyRepositoryIT extends SharedPostgresContainer {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Test
    @Transactional
    void rejectsDuplicateActiveProvinceCode() {
        ProvinceEntity original = newProvince("GEO-DUP-1", "Geo Dup 1");
        provinceRepository.saveAndFlush(original);

        ProvinceEntity duplicate = newProvince("GEO-DUP-1", "Geo Dup 1 Copy");
        assertThatThrownBy(() -> provinceRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void allowsReuseOfProvinceCodeAfterSoftDelete() {
        ProvinceEntity original = provinceRepository.saveAndFlush(newProvince("GEO-SOFT-1", "Geo Soft 1"));
        original.setDeleted(true);
        original.setStatus("DISABLED");
        provinceRepository.saveAndFlush(original);

        ProvinceEntity replacement = provinceRepository.saveAndFlush(newProvince("GEO-SOFT-1", "Geo Soft 1 Reuse"));
        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        assertThat(provinceRepository.findByCodeAndDeletedFalse("GEO-SOFT-1")).isPresent();
        assertThat(provinceRepository.findByIdAndDeletedFalse(original.getId())).isEmpty();
    }

    @Test
    @Transactional
    void rejectsDuplicateActiveDistrictCode() {
        UUID northId = provinceRepository.findByCodeAndDeletedFalse("NORTH").orElseThrow().getId();
        DistrictEntity original = newDistrict("GEO-DIST-DUP", "Geo Dist Dup", northId);
        districtRepository.saveAndFlush(original);

        DistrictEntity duplicate = newDistrict("GEO-DIST-DUP", "Geo Dist Dup Copy", northId);
        assertThatThrownBy(() -> districtRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void rejectsDistrictWithUnknownProvince() {
        DistrictEntity district = newDistrict(
            "GEO-DIST-FK",
            "Geo Dist Fk",
            UUID.fromString("00000000-0000-4000-8000-000000000099")
        );
        assertThatThrownBy(() -> districtRepository.saveAndFlush(district))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void rejectsFarmWithUnknownDistrict() {
        FarmerEntity farmer = farmerRepository.saveAndFlush(
            FarmerEntity.create("Geo", "Fk", "NID-FARM-FK-1", "+250788000101")
        );
        FarmEntity farm = FarmEntity.create(farmer.getId(), "FARM-FK-BAD-1", "Unknown district farm");
        farm.setDistrictId(UUID.fromString("00000000-0000-4000-8000-000000000099"));
        assertThatThrownBy(() -> farmRepository.saveAndFlush(farm))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void allowsFarmWithNullDistrict() {
        FarmerEntity farmer = farmerRepository.saveAndFlush(
            FarmerEntity.create("Geo", "Null", "NID-FARM-NULL-1", "+250788000102")
        );
        FarmEntity farm = FarmEntity.create(farmer.getId(), "FARM-FK-NULL-1", "Nullable district farm");
        farm.setDistrictId(null);
        FarmEntity saved = farmRepository.saveAndFlush(farm);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDistrictId()).isNull();
    }

    @Test
    @Transactional
    void acceptsFarmWithSeededDistrict() {
        UUID musanzeId = districtRepository.findByCodeAndDeletedFalse("MUSANZE").orElseThrow().getId();
        FarmerEntity farmer = farmerRepository.saveAndFlush(
            FarmerEntity.create("Geo", "Ok", "NID-FARM-OK-1", "+250788000103")
        );
        FarmEntity farm = FarmEntity.create(farmer.getId(), "FARM-FK-OK-1", "Musanze farm");
        farm.setDistrictId(musanzeId);
        FarmEntity saved = farmRepository.saveAndFlush(farm);
        assertThat(saved.getDistrictId()).isEqualTo(musanzeId);
    }

    private static ProvinceEntity newProvince(String code, String name) {
        ProvinceEntity province = new ProvinceEntity();
        province.setCode(code);
        province.setName(name);
        province.setDeleted(false);
        province.setStatus("ACTIVE");
        return province;
    }

    private static DistrictEntity newDistrict(String code, String name, UUID provinceId) {
        DistrictEntity district = new DistrictEntity();
        district.setCode(code);
        district.setName(name);
        district.setProvinceId(provinceId);
        district.setDeleted(false);
        district.setStatus("ACTIVE");
        return district;
    }
}
