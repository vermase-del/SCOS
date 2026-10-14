package com.scos.modules.maintenance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scos_assets", uniqueConstraints = @UniqueConstraint(name = "uq_scos_asset_community_code", columnNames = {"community_id", "asset_code"}), indexes = {
    @Index(name = "idx_scos_assets_community_type_status", columnList = "community_id,asset_type,status"),
    @Index(name = "idx_scos_assets_next_maintenance", columnList = "community_id,next_maintenance_at")
})
public class AssetEntity {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "community_id", nullable = false, updatable = false) private UUID communityId;
    @Column(name = "asset_code", nullable = false, length = 40) private String assetCode;
    @Column(nullable = false, length = 160) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "asset_type", nullable = false, length = 50) private String assetType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AssetStatus status = AssetStatus.OPERATIONAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AssetCriticality criticality = AssetCriticality.MEDIUM;
    @Column(name = "building_id") private UUID buildingId;
    @Column(name = "unit_id") private UUID unitId;
    @Column(name = "location_details", length = 200) private String locationDetails;
    @Column(length = 100) private String manufacturer;
    @Column(name = "model_number", length = 100) private String modelNumber;
    @Column(name = "serial_number", length = 120) private String serialNumber;
    @Column(name = "installed_on") private LocalDate installedOn;
    @Column(name = "warranty_expires_on") private LocalDate warrantyExpiresOn;
    @Column(name = "last_maintenance_at") private Instant lastMaintenanceAt;
    @Column(name = "next_maintenance_at") private Instant nextMaintenanceAt;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "created_by", nullable = false, length = 60, updatable = false) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_by", nullable = false, length = 60) private String updatedBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    @PrePersist void beforeInsert() { Instant now = Instant.now(); if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; if (createdBy == null) createdBy = "system"; if (updatedBy == null) updatedBy = createdBy; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getCommunityId(){return communityId;} public void setCommunityId(UUID v){communityId=v;}
    public String getAssetCode(){return assetCode;} public void setAssetCode(String v){assetCode=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getAssetType(){return assetType;} public void setAssetType(String v){assetType=v;}
    public AssetStatus getStatus(){return status;} public void setStatus(AssetStatus v){status=v;}
    public AssetCriticality getCriticality(){return criticality;} public void setCriticality(AssetCriticality v){criticality=v;}
    public UUID getBuildingId(){return buildingId;} public void setBuildingId(UUID v){buildingId=v;}
    public UUID getUnitId(){return unitId;} public void setUnitId(UUID v){unitId=v;}
    public String getLocationDetails(){return locationDetails;} public void setLocationDetails(String v){locationDetails=v;}
    public String getManufacturer(){return manufacturer;} public void setManufacturer(String v){manufacturer=v;}
    public String getModelNumber(){return modelNumber;} public void setModelNumber(String v){modelNumber=v;}
    public String getSerialNumber(){return serialNumber;} public void setSerialNumber(String v){serialNumber=v;}
    public LocalDate getInstalledOn(){return installedOn;} public void setInstalledOn(LocalDate v){installedOn=v;}
    public LocalDate getWarrantyExpiresOn(){return warrantyExpiresOn;} public void setWarrantyExpiresOn(LocalDate v){warrantyExpiresOn=v;}
    public Instant getLastMaintenanceAt(){return lastMaintenanceAt;} public void setLastMaintenanceAt(Instant v){lastMaintenanceAt=v;}
    public Instant getNextMaintenanceAt(){return nextMaintenanceAt;} public void setNextMaintenanceAt(Instant v){nextMaintenanceAt=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt( v){updatedAt=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
}
