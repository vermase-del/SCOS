package com.scos.modules.maintenance.dto;
public record WorkOrderSummaryDto(long totalActive,long openCount,long assignedCount,long inProgressCount,long onHoldCount,long resolvedCount,long slaBreachedCount,double averageRating) {}
