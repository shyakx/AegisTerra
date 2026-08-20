package com.aegisterra.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.support.SharedPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class FarmerRepositoryIT extends SharedPostgresContainer {

    @Autowired
    private FarmerRepository farmerRepository;

    @Test
    @Transactional
    void rejectsDuplicateActiveNationalId() {
        farmerRepository.save(FarmerEntity.create("Aline", "Mukamana", "NID-DUP-001", "+250788000010"));
        farmerRepository.flush();

        FarmerEntity duplicate = FarmerEntity.create("Aline", "Other", "NID-DUP-001", "+250788000011");
        assertThatThrownBy(() -> {
            farmerRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void allowsReuseOfNationalIdAfterSoftDelete() {
        FarmerEntity original = farmerRepository.save(
            FarmerEntity.create("Eric", "Niyonzima", "NID-SOFT-001", "+250788000020")
        );
        original.setDeleted(true);
        original.setStatus("DELETED");
        farmerRepository.saveAndFlush(original);

        FarmerEntity replacement = farmerRepository.saveAndFlush(
            FarmerEntity.create("Eric", "Niyonzima", "NID-SOFT-001", "+250788000021")
        );

        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        assertThat(farmerRepository.findByNationalIdAndDeletedFalse("NID-SOFT-001")).isPresent();
    }
}
