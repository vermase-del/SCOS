package com.scos.modules.maintenance.service;
import com.scos.modules.maintenance.entity.WorkOrderStatus; import org.springframework.stereotype.Component; import java.util.*;
@Component public class WorkOrderStateMachine {
 private static final Map<WorkOrderStatus,Set<WorkOrderStatus>> ALLOWED=Map.of(
 WorkOrderStatus.OPEN,Set.of(WorkOrderStatus.ASSIGNED,WorkOrderStatus.CANCELLED),
 WorkOrderStatus.ASSIGNED,Set.of(WorkOrderStatus.ASSIGNED,WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.CANCELLED),
 WorkOrderStatus.IN_PROGRESS,Set.of(WorkOrderStatus.ON_HOLD,WorkOrderStatus.RESOLVED,WorkOrderStatus.ASSIGNED),
 WorkOrderStatus.ON_HOLD,Set.of(WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.CANCELLED),
 WorkOrderStatus.RESOLVED,Set.of(WorkOrderStatus.CLOSED,WorkOrderStatus.IN_PROGRESS),
 WorkOrderStatus.CLOSED,Set.of(), WorkOrderStatus.CANCELLED,Set.of());
 public void validateTransition(WorkOrderStatus from,WorkOrderStatus to,String roleCode,String notes){if(from==null||to==null||!ALLOWED.getOrDefault(from,Set.of()).contains(to))throw new IllegalArgumentException("Invalid work-order transition: "+from+" -> "+to);if(to==WorkOrderStatus.ON_HOLD&&(notes==null||notes.isBlank()))throw new IllegalArgumentException("A hold reason is required");if(to==WorkOrderStatus.CLOSED&&!Set.of("RESIDENT","COMMUNITY_MANAGER","SUPER_ADMIN").contains(roleCode))throw new IllegalArgumentException("Only a resident or community manager can close a resolved work order");}
}
