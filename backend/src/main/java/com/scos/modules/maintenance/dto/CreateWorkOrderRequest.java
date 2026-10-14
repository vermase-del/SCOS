package com.scos.modules.maintenance.dto;
import com.scos.modules.maintenance.entity.*; import jakarta.validation.constraints.*; import java.util.UUID;
public record CreateWorkOrderRequest(@NotNull UUID communityId,@NotBlank @Size(max=160) String title,@NotBlank String description,@NotNull WorkOrderScope scope,UUID buildingId,UUID unitId,@Size(max=200) String locationDetails,@NotNull WorkOrderCategory category,@NotNull WorkOrderPriority priority) {}
