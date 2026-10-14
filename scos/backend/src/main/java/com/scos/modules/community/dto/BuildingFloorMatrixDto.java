package com.scos.modules.community.dto;

import java.util.List;
import java.util.UUID;

public class BuildingFloorMatrixDto {

    private UUID buildingId;
    private String buildingCode;
    private String buildingName;
    private int totalFloors;
    private int totalUnits;
    private List<FloorRowDto> floors;

    public BuildingFloorMatrixDto() {}

    public BuildingFloorMatrixDto(UUID buildingId, String buildingCode, String buildingName, int totalFloors, int totalUnits, List<FloorRowDto> floors) {
        this.buildingId = buildingId;
        this.buildingCode = buildingCode;
        this.buildingName = buildingName;
        this.totalFloors = totalFloors;
        this.totalUnits = totalUnits;
        this.floors = floors;
    }

    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }

    public String getBuildingCode() { return buildingCode; }
    public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public int getTotalFloors() { return totalFloors; }
    public void setTotalFloors(int totalFloors) { this.totalFloors = totalFloors; }

    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }

    public List<FloorRowDto> getFloors() { return floors; }
    public void setFloors(List<FloorRowDto> floors) { this.floors = floors; }

    public static class FloorRowDto {
        private int floorNumber;
        private List<UnitTileDto> units;

        public FloorRowDto() {}
        public FloorRowDto(int floorNumber, List<UnitTileDto> units) {
            this.floorNumber = floorNumber;
            this.units = units;
        }

        public int getFloorNumber() { return floorNumber; }
        public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

        public List<UnitTileDto> getUnits() { return units; }
        public void setUnits(List<UnitTileDto> units) { this.units = units; }
    }

    public static class UnitTileDto {
        private UUID id;
        private String unitNumber;
        private String unitType;
        private String occupancyStatus;
        private String primaryResidentName;

        public UnitTileDto() {}
        public UnitTileDto(UUID id, String unitNumber, String unitType, String occupancyStatus, String primaryResidentName) {
            this.id = id;
            this.unitNumber = unitNumber;
            this.unitType = unitType;
            this.occupancyStatus = occupancyStatus;
            this.primaryResidentName = primaryResidentName;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getUnitNumber() { return unitNumber; }
        public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }

        public String getOccupancyStatus() { return occupancyStatus; }
        public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; }

        public String getPrimaryResidentName() { return primaryResidentName; }
        public void setPrimaryResidentName(String primaryResidentName) { this.primaryResidentName = primaryResidentName; }
    }
}
