package com.aegisterra.platform.infrastructure.persistence.agriculture;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "farmers")
public class FarmerEntity extends AuditableEntity {

    @Column(name = "household_id")
    private UUID householdId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "aggregator_id")
    private UUID aggregatorId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "farmer_code", length = 64)
    private String farmerCode;

    @Column(name = "national_id", nullable = false, length = 32)
    private String nationalId;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column
    private String email;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "sector_id")
    private UUID sectorId;

    @Column(name = "cell_id")
    private UUID cellId;

    @Column(name = "village_id")
    private UUID villageId;

    public static FarmerEntity create(
        String firstName,
        String lastName,
        String nationalId,
        String phoneNumber
    ) {
        FarmerEntity farmer = new FarmerEntity();
        farmer.setFirstName(firstName);
        farmer.setLastName(lastName);
        farmer.setNationalId(nationalId);
        farmer.setPhoneNumber(phoneNumber);
        farmer.setStatus("PENDING_VERIFICATION");
        farmer.setDeleted(false);
        return farmer;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(UUID householdId) {
        this.householdId = householdId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getAggregatorId() {
        return aggregatorId;
    }

    public void setAggregatorId(UUID aggregatorId) {
        this.aggregatorId = aggregatorId;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFarmerCode() {
        return farmerCode;
    }

    public void setFarmerCode(String farmerCode) {
        this.farmerCode = farmerCode;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getDistrictId() {
        return districtId;
    }

    public void setDistrictId(UUID districtId) {
        this.districtId = districtId;
    }

    public UUID getSectorId() {
        return sectorId;
    }

    public void setSectorId(UUID sectorId) {
        this.sectorId = sectorId;
    }

    public UUID getCellId() {
        return cellId;
    }

    public void setCellId(UUID cellId) {
        this.cellId = cellId;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public void setVillageId(UUID villageId) {
        this.villageId = villageId;
    }
}
