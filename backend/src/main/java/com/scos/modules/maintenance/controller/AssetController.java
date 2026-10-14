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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name="Asset Register", description="Community-scoped asset inventory and maintenance scheduling")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service=service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:WRITE')")
    @Operation(summary="Register an asset in a community")
    public ApiResponse<AssetDto> create(@RequestParam UUID communityId,
        @Valid @RequestBody CreateAssetRequest request, Principal principal) {
        return ApiResponse.success("Asset registered",service.create(communityId,request,principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="List active assets in a community")
    public ApiResponse<PageResponse<AssetDto>> list(@RequestParam UUID communityId,
        @RequestParam(required=false) AssetStatus status,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Principal principal) {
        Page<AssetDto> result=service.list(communityId,status,page,size,principal.getName());
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasAuthority('ASSET:READ') or hasAuthority('SCREEN:WORK_ORDERS')")
    @Operation(summary="Get asset details")
    public ApiResponse<AssetDto> get(@PathVariable UUID id,@RequestParam UUID communityId,Principal principal) {
        return ApiResponse.success(service.get(id,communityId,principal.getName()));
    }
}
