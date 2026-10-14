package com.scos.modules.community.dto;

import java.util.UUID;

public class BuildingDto {
    private UUID id;
    private UUID communityId;
    private String code;
    private String name;
    private int totalFloors;
    private int basementFloors;
    private int totalUnits;
    private boolean hasElevator;
    private String constructionStatus;

    public BuildingDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalFloors() { return totalFloors; }
    public void setTotalFloors(int totalFloors) { this.totalFloors = totalFloors; }

    public int getBasementFloors() { return basementFloors; }
    public void setBasementFloors(int basementFloors) { this.basementFloors = basementFloors; }

    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }

    public boolean isHasElevator() { return hasElevator; }
    public void setHasElevator(boolean hasElevator) { this.hasElevator = hasElevator; }

    public String getConstructionStatus() { return constructionStatus; }
    public void setConstructionStatus(String constructionStatus) { this.constructionStatus = constructionStatus; }
}
