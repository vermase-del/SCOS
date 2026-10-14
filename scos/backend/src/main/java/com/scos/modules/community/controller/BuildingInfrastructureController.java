package com.scos.modules.community.controller;

import com.scos.common.model.ApiResponse;
import com.scos.common.model.PageResponse;
import com.scos.modules.community.dto.BuildingDto;
import com.scos.modules.community.dto.BuildingFloorMatrixDto;
import com.scos.modules.community.dto.UnitDto;
import com.scos.modules.community.service.BuildingInfrastructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/infrastructure")
@Tag(name = "Building Infrastructure", description = "Towers, floor matrices, and unit inventory")
public class BuildingInfrastructureController {

    private final BuildingInfrastructureService infrastructureService;

    public BuildingInfrastructureController(BuildingInfrastructureService infrastructureService) {
        this.infrastructureService = infrastructureService;
    }

    @GetMapping("/buildings")
    @Operation(summary = "Get towers/blocks for active community")
    public ApiResponse<List<BuildingDto>> getBuildings() {
        return ApiResponse.success(infrastructureService.getBuildingsForCurrentCommunity());
    }

    @GetMapping("/buildings/{buildingId}/matrix")
    @Operation(summary = "Get 2D visual floor matrix for a residential tower")
    public ApiResponse<BuildingFloorMatrixDto> getBuildingFloorMatrix(@PathVariable UUID buildingId) {
        return ApiResponse.success(infrastructureService.getBuildingFloorMatrix(buildingId));
    }

    @GetMapping("/units")
    @Operation(summary = "Search units with optional building and occupancy filters")
    public ApiResponse<PageResponse<UnitDto>> getUnits(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) String occupancyStatus,
            Pageable pageable) {
        return ApiResponse.success(PageResponse.from(infrastructureService.getUnits(buildingId, occupancyStatus, pageable)));
    }

    @PatchMapping("/units/{unitId}/status")
    @PreAuthorize("hasAuthority('UNIT:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Update unit occupancy or maintenance status")
    public ApiResponse<UnitDto> updateUnitStatus(
            @PathVariable UUID unitId,
            @RequestParam String occupancyStatus) {
        return ApiResponse.success("Unit status updated", infrastructureService.updateUnitStatus(unitId, occupancyStatus));
    }
}
