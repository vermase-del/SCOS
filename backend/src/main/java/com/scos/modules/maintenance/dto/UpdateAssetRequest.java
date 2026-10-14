package com.scos.modules.maintenance.dto;

import com.scos.modules.maintenance.entity.AssetCriticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateAssetRequest(
    @NotBlank @Size(max=160) String name,
    String description,
    @NotBlank @Size(max=50) String assetType,
    AssetCriticality criticality,
    UUID buildingId,
    UUID unitId,
    @Size(max=200) String locationDetails,
    @Size(max=100) String manufacturer,
    @Size(max=100) String modelNumber,
    @Size(max=120) String serialNumber,
    LocalDate installedOn,
    LocalDate warrantyExpiresOn,
    Instant nextMaintenanceAt
) {}
