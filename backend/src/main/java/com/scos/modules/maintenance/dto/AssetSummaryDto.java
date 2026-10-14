package com.scos.modules.maintenance.dto;

public record AssetSummaryDto(
    long totalAssets,
    long operationalCount,
    long underMaintenanceCount,
    long outOfServiceCount,
    long retiredCount,
    long criticalCount,
    long upcomingMaintenanceCount
) {}
