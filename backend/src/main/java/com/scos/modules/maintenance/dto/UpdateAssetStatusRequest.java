package com.scos.modules.maintenance.dto;

import com.scos.modules.maintenance.entity.AssetStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAssetStatusRequest(
    @NotNull AssetStatus status,
    String reason
) {}
