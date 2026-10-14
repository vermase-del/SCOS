package com.scos.modules.maintenance.controller;

import com.scos.common.model.ApiResponse;
import com.scos.common.model.PageResponse;
import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.WorkOrderPriority;
import com.scos.modules.maintenance.entity.WorkOrderStatus;
import com.scos.modules.maintenance.entity.WorkOrderType;
import com.scos.modules.maintenance.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
@Tag(name="Complaint & Maintenance Tracking", description="Tenant-scoped work order lifecycle, SLA, dispatch and cost tracking")
public class WorkOrderController {
    private final WorkOrderService service;
    public WorkOrderController(WorkOrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS') or hasAuthority('WORKORDER:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="Create a community work order")
    public ApiResponse<WorkOrderDto> create(@Valid @RequestBody CreateWorkOrderRequest request, Principal principal) {
        return ApiResponse.success("Work order created", service.createWorkOrder(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS') or hasAuthority('WORKORDER:READ') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="List work orders within a community")
    public ApiResponse<PageResponse<WorkOrderDto>> list(@RequestParam UUID communityId,
        @RequestParam(required=false) WorkOrderStatus status,
        @RequestParam(required=false) WorkOrderPriority priority,
        @RequestParam(required=false) WorkOrderType workOrderType,
        @RequestParam(required=false) UUID assetId,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, Principal principal) {
        Page<WorkOrderDto> result = service.getWorkOrders(communityId, status, priority, workOrderType, assetId, page, size, principal.getName());
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS') or hasAuthority('WORKORDER:READ') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="Get work order operations KPIs")
    public ApiResponse<WorkOrderSummaryDto> summary(@RequestParam UUID communityId, Principal principal) {
        return ApiResponse.success(service.getSummary(communityId, principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS') or hasAuthority('WORKORDER:READ') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="Get work order details and timeline")
    public ApiResponse<WorkOrderDto> get(@PathVariable UUID id, @RequestParam UUID communityId, Principal principal) {
        return ApiResponse.success(service.getWorkOrder(id, communityId, principal.getName()));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="Assign or reassign a technician")
    public ApiResponse<WorkOrderDto> assign(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody AssignTechnicianRequest request, Principal principal) {
        return ApiResponse.success("Technician assigned", service.assignTechnician(id, communityId, request, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('WORKORDER:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('TECHNICIAN') or hasRole('RESIDENT')")
    @Operation(summary="Apply a validated work order state transition")
    public ApiResponse<WorkOrderDto> transition(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody TransitionStatusRequest request, Principal principal) {
        return ApiResponse.success("Work order status updated", service.transitionStatus(id, communityId, request, principal.getName()));
    }

    @PostMapping("/{id}/costs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('TECHNICIAN')")
    @Operation(summary="Record an itemized cost line")
    public ApiResponse<WorkOrderCostLineDto> addCost(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody AddCostLineRequest request, Principal principal) {
        return ApiResponse.success("Cost recorded", service.addCostLine(id, communityId, request, principal.getName()));
    }

    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS') or hasRole('RESIDENT') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary="Submit resident resolution verification and rating")
    public ApiResponse<WorkOrderDto> feedback(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody SubmitFeedbackRequest request, Principal principal) {
        return ApiResponse.success("Feedback recorded", service.submitFeedback(id, communityId, request, principal.getName()));
    }
}
