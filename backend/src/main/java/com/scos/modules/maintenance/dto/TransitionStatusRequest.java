package com.scos.modules.maintenance.dto;
import com.scos.modules.maintenance.entity.WorkOrderStatus; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size;
public record TransitionStatusRequest(@NotNull WorkOrderStatus targetStatus,@Size(max=2000) String notes) {}
