package com.scos.modules.community.entity;

import com.scos.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "scos_units")
public class UnitEntity extends AuditableEntity {

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "unit_type", nullable = false, length = 30)
    private String unitType;

    @Column(name = "carpet_area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal carpetAreaSqft;

    @Column(name = "super_builtup_area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal superBuiltupAreaSqft;

    @Column(name = "balcony_count", nullable = false)
    private int balconyCount = 1;

    @Column(name = "bathroom_count", nullable = false)
    private int bathroomCount = 2;

    @Column(name = "parking_slot_number", length = 50)
    private String parkingSlotNumber;

    @Column(name = "occupancy_status", nullable = false, length = 25)
    private String occupancyStatus = "VACANT";

    @Column(name = "furnishing_status", nullable = false, length = 20)
    private String furnishingStatus = "SEMI_FURNISHED";

    @Column(name = "electric_meter_number", length = 50)
    private String electricMeterNumber;

    @Column(name = "gas_meter_number", length = 50)
    private String gasMeterNumber;

    @Column(name = "water_meter_number", length = 50)
    private String waterMeterNumber;

    @Column(name = "base_maintenance_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseMaintenanceCharge = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UnitEntity() {}

    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }

    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public BigDecimal getCarpetAreaSqft() { return carpetAreaSqft; }
    public void setCarpetAreaSqft(BigDecimal carpetAreaSqft) { this.carpetAreaSqft = carpetAreaSqft; }

    public BigDecimal getSuperBuiltupAreaSqft() { return superBuiltupAreaSqft; }
    public void setSuperBuiltupAreaSqft(BigDecimal superBuiltupAreaSqft) { this.superBuiltupAreaSqft = superBuiltupAreaSqft; }

    public int getBalconyCount() { return balconyCount; }
    public void setBalconyCount(int balconyCount) { this.balconyCount = balconyCount; }

    public int getBathroomCount() { return bathroomCount; }
    public void setBathroomCount(int bathroomCount) { this.bathroomCount = bathroomCount; }

    public String getParkingSlotNumber() { return parkingSlotNumber; }
    public void setParkingSlotNumber(String parkingSlotNumber) { this.parkingSlotNumber = parkingSlotNumber; }

    public String getOccupancyStatus() { return occupancyStatus; }
    public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; }

    public String getFurnishingStatus() { return furnishingStatus; }
    public void setFurnishingStatus(String furnishingStatus) { this.furnishingStatus = furnishingStatus; }

    public String getElectricMeterNumber() { return electricMeterNumber; }
    public void setElectricMeterNumber(String electricMeterNumber) { this.electricMeterNumber = electricMeterNumber; }

    public String getGasMeterNumber() { return gasMeterNumber; }
    public void setGasMeterNumber(String gasMeterNumber) { this.gasMeterNumber = gasMeterNumber; }

    public String getWaterMeterNumber() { return waterMeterNumber; }
    public void setWaterMeterNumber(String waterMeterNumber) { this.waterMeterNumber = waterMeterNumber; }

    public BigDecimal getBaseMaintenanceCharge() { return baseMaintenanceCharge; }
    public void setBaseMaintenanceCharge(BigDecimal baseMaintenanceCharge) { this.baseMaintenanceCharge = baseMaintenanceCharge; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
