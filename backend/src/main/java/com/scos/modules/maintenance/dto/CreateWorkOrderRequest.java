package com.scos.modules.maintenance.dto;

import com.scos.modules.maintenance.entity.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateWorkOrderRequest(
    @NotNull UUID communityId,
    @NotBlank @Size(max=160) String title,
    @NotBlank String description,
    @NotNull WorkOrderScope scope,
    UUID buildingId,
    UUID unitId,
    @Size(max=200) String locationDetails,
    @NotNull WorkOrderCategory category,
    @NotNull WorkOrderPriority priority,
    WorkOrderType workOrderType,
    UUID assetId,
    @Size(max=40) String reasonCode,
    @Size(max=50) String departmentCode,
    Instant plannedStartAt,
    Instant plannedEndAt,
    Boolean outOfService,
    @Size(max=40) String serviceType,
    @Size(max=120) String externalReference
) {
    public CreateWorkOrderRequest(
        UUID communityId, String title, String description, WorkOrderScope scope,
        UUID buildingId, UUID unitId, String locationDetails, WorkOrderCategory category,
        WorkOrderPriority priority
    ) {
        this(communityId, title, description, scope, buildingId, unitId, locationDetails, category, priority,
             WorkOrderType.REACTIVE, null, null, null, null, null, false, null, null);
    }
}
