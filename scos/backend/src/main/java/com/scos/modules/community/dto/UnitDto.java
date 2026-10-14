package com.scos.modules.community.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UnitDto {
    private UUID id;
    private UUID communityId;
    private UUID buildingId;
    private String unitNumber;
    private int floorNumber;
    private String unitType;
    private BigDecimal carpetAreaSqft;
    private BigDecimal superBuiltupAreaSqft;
    private int balconyCount;
    private int bathroomCount;
    private String parkingSlotNumber;
    private String occupancyStatus;
    private String furnishingStatus;
    private String electricMeterNumber;
    private String gasMeterNumber;
    private String waterMeterNumber;
    private BigDecimal baseMaintenanceCharge;
    private boolean active;

    public UnitDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
