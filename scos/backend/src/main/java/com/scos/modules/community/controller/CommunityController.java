package com.scos.modules.community.controller;

import com.scos.common.model.ApiResponse;
import com.scos.modules.community.dto.CommunityDto;
import com.scos.modules.community.dto.CommunityRegistrationRequest;
import com.scos.modules.community.dto.CommunityRegistrationResponse;
import com.scos.modules.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities")
@Tag(name = "Community Management", description = "Residential societies, complexes, and associations")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new residential society/community with administrator and optional rapid infrastructure provisioning")
    public ApiResponse<CommunityRegistrationResponse> registerCommunity(
            @Valid @RequestBody CommunityRegistrationRequest request) {
        CommunityRegistrationResponse response = communityService.registerCommunity(request);
        return ApiResponse.success("Society registered successfully", response);
    }

    @GetMapping
    @Operation(summary = "List all communities accessible to user")
    public ApiResponse<List<CommunityDto>> getCommunities() {
        return ApiResponse.success(communityService.getAllCommunities());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get community details by ID")
    public ApiResponse<CommunityDto> getCommunityById(@PathVariable UUID id) {
        return ApiResponse.success(communityService.getCommunityById(id));
    }
}
