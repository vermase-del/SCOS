package com.scos.modules.maintenance.dto;
import com.scos.modules.maintenance.entity.*; import java.time.Instant; import java.util.UUID;
public record WorkOrderActivityDto(UUID id,UUID authorUserId,ActivityType activityType,WorkOrderStatus fromStatus,WorkOrderStatus toStatus,String notes,Instant createdAt) {}
