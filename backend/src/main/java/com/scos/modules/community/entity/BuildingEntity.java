package com.scos.modules.community.entity;

import com.scos.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scos_buildings")
public class BuildingEntity extends AuditableEntity {

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "total_floors", nullable = false)
    private int totalFloors;

    @Column(name = "basement_floors", nullable = false)
    private int basementFloors = 1;

    @Column(name = "total_units", nullable = false)
    private int totalUnits = 0;

    @Column(name = "has_elevator", nullable = false)
    private boolean hasElevator = true;

    @Column(name = "handover_date")
    private LocalDate handoverDate;

    @Column(name = "construction_status", nullable = false, length = 25)
    private String constructionStatus = "COMPLETED";

    public BuildingEntity() {}

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

    public LocalDate getHandoverDate() { return handoverDate; }
    public void setHandoverDate(LocalDate handoverDate) { this.handoverDate = handoverDate; }

    public String getConstructionStatus() { return constructionStatus; }
    public void setConstructionStatus(String constructionStatus) { this.constructionStatus = constructionStatus; }
}
