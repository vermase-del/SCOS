package com.scos.modules.maintenance.controller;

import com.scos.common.model.ApiResponse;
import com.scos.common.model.PageResponse;
import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.AssetStatus;
import com.scos.modules.maintenance.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name="Asset Register", description="Community-scoped asset inventory and maintenance scheduling")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:WRITE') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Register an asset in a community")
    public ApiResponse<AssetDto> create(@RequestParam UUID communityId,
        @Valid @RequestBody CreateAssetRequest request, Principal principal) {
        return ApiResponse.success("Asset registered", service.create(communityId, request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="List active assets in a community")
    public ApiResponse<PageResponse<AssetDto>> list(@RequestParam UUID communityId,
        @RequestParam(required=false) AssetStatus status,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, Principal principal) {
        Page<AssetDto> result = service.list(communityId, status, page, size, principal.getName());
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Get asset KPI metrics summary")
    public ApiResponse<AssetSummaryDto> summary(@RequestParam UUID communityId, Principal principal) {
        return ApiResponse.success(service.getSummary(communityId, principal.getName()));
    }

    @GetMapping("/due-maintenance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Get assets due for preventive maintenance")
    public ApiResponse<PageResponse<AssetDto>> getDueForMaintenance(@RequestParam UUID communityId,
        @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant threshold,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, Principal principal) {
        Page<AssetDto> result = service.getDueForMaintenance(communityId, threshold, page, size, principal.getName());
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Get asset details")
    public ApiResponse<AssetDto> get(@PathVariable UUID id, @RequestParam UUID communityId, Principal principal) {
        return ApiResponse.success(service.get(id, communityId, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:WRITE') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Update asset details")
    public ApiResponse<AssetDto> update(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody UpdateAssetRequest request, Principal principal) {
        return ApiResponse.success("Asset updated", service.update(id, communityId, request, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('TECHNICIAN') or hasAuthority('ASSET:WRITE') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Update asset status and record audit event")
    public ApiResponse<AssetDto> updateStatus(@PathVariable UUID id, @RequestParam UUID communityId,
        @Valid @RequestBody UpdateAssetStatusRequest request, Principal principal) {
        return ApiResponse.success("Asset status updated", service.updateStatus(id, communityId, request, principal.getName()));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Get append-only asset status transition history")
    public ApiResponse<List<AssetStatusHistoryDto>> getStatusHistory(@PathVariable UUID id, @RequestParam UUID communityId, Principal principal) {
        return ApiResponse.success(service.getStatusHistory(id, communityId, principal.getName()));
    }
}
