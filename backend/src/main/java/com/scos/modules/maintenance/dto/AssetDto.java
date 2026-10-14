package com.scos.modules.maintenance.dto;

import com.scos.modules.maintenance.entity.AssetCriticality;
import com.scos.modules.maintenance.entity.AssetStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssetDto(UUID id, UUID communityId, String assetCode, String name, String description,
    String assetType, AssetStatus status, AssetCriticality criticality, UUID buildingId, UUID unitId,
    String locationDetails, String manufacturer, String modelNumber, String serialNumber,
    LocalDate installedOn, LocalDate warrantyExpiresOn, Instant lastMaintenanceAt,
    Instant nextMaintenanceAt, boolean active, long version, Instant createdAt, Instant updatedAt) {}
