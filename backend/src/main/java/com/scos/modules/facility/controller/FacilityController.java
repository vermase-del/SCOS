package com.scos.modules.facility.controller;

import com.scos.common.model.ApiResponse;
import com.scos.modules.facility.dto.CreateFacilityRequest;
import com.scos.modules.facility.dto.FacilityDto;
import com.scos.modules.facility.dto.FacilitySummaryDto;
import com.scos.modules.facility.dto.UpdateFacilityRequest;
import com.scos.modules.facility.entity.FacilityCategory;
import com.scos.modules.facility.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facilities")
@Tag(name = "Facility & Amenity Management", description = "Community facility catalog, operating hours, capacity, and configuration")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "List facilities for a community with optional category/active filtering")
    public ApiResponse<List<FacilityDto>> listFacilities(
            @RequestParam UUID communityId,
            @RequestParam(required = false) FacilityCategory category,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success(facilityService.listFacilities(communityId, category, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Get facility details by ID")
    public ApiResponse<FacilityDto> getFacility(
            @PathVariable UUID id,
            @RequestParam UUID communityId) {
        return ApiResponse.success(facilityService.getFacility(communityId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('HOA_OFFICER')")
    @Operation(summary = "Create a new facility or amenity in the community")
    public ApiResponse<FacilityDto> createFacility(
            @RequestParam UUID communityId,
            @Valid @RequestBody CreateFacilityRequest request,
            Principal principal) {
        return ApiResponse.success("Facility created successfully",
                facilityService.createFacility(communityId, request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('HOA_OFFICER')")
    @Operation(summary = "Update facility attributes, operating rules, or status")
    public ApiResponse<FacilityDto> updateFacility(
            @PathVariable UUID id,
            @RequestParam UUID communityId,
            @Valid @RequestBody UpdateFacilityRequest request,
            Principal principal) {
        return ApiResponse.success("Facility updated successfully",
                facilityService.updateFacility(communityId, id, request, principal.getName()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('HOA_OFFICER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Get facility operations KPIs and category distribution")
    public ApiResponse<FacilitySummaryDto> getSummary(@RequestParam UUID communityId) {
        return ApiResponse.success(facilityService.getFacilitySummary(communityId));
    }
}
