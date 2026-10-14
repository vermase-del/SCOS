package com.scos.modules.maintenance.dto;

import com.scos.modules.maintenance.entity.AssetStatus;
import java.time.Instant;
import java.util.UUID;

public record AssetStatusHistoryDto(
    UUID id,
    UUID assetId,
    AssetStatus fromStatus,
    AssetStatus toStatus,
    String reason,
    UUID changedByUserId,
    Instant changedAt
) {}
