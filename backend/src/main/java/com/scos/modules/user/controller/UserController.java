package com.scos.modules.user.controller;

import com.scos.common.model.ApiResponse;
import com.scos.common.model.PageResponse;
import com.scos.modules.user.dto.*;
import com.scos.modules.user.service.UserService;
import com.scos.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User & Permission Management", description = "User directory, RBAC screen permissions, and role configurations")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public UserController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCREEN:USER_DIRECTORY') or hasAuthority('USER:READ') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Get paginated user directory")
    public ApiResponse<PageResponse<UserDto>> getUsers(Pageable pageable) {
        return ApiResponse.success(PageResponse.from(userService.getUsers(pageable)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCREEN:USER_CREATE') or hasAuthority('USER:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Invite and provision a new user")
    public ApiResponse<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created successfully", userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ApiResponse<UserDto> getUserById(@PathVariable UUID id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Assign role in community to user")
    public ApiResponse<Void> assignRole(
            @PathVariable UUID id,
            @RequestParam UUID communityId,
            @RequestParam String roleCode) {
        userService.assignRoleToUser(id, communityId, roleCode);
        return ApiResponse.success("Role assigned successfully", null);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER:WRITE') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Activate or deactivate a user account")
    public ApiResponse<Void> toggleStatus(@PathVariable UUID id, @RequestParam boolean active) {
        userService.toggleUserActiveStatus(id, active);
        return ApiResponse.success("User status updated", null);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('SCREEN:ROLE_MANAGEMENT') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Get all roles with their assigned screen and feature permissions")
    public ApiResponse<List<RoleDto>> getAllRoles() {
        return ApiResponse.success(userService.getAllRolesWithPermissions());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('SCREEN:ROLE_MANAGEMENT') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Get all available system permissions categorized by module")
    public ApiResponse<List<PermissionDto>> getAllPermissions() {
        return ApiResponse.success(userService.getAllPermissions());
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority('SCREEN:ROLE_MANAGEMENT') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "Update and customize screen & capability permissions for a specific role")
    public ApiResponse<RoleDto> updateRolePermissions(
            @PathVariable String roleCode,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        RoleDto updated = userService.updateRolePermissions(roleCode, request.getPermissionCodes());
        return ApiResponse.success("Role permissions updated successfully", updated);
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current authenticated user's effective permissions")
    public ApiResponse<List<String>> getCurrentUserPermissions(HttpServletRequest request) {
        String token = tokenProvider.extractToken(request);
        if (token != null) {
            UUID userId = tokenProvider.getUserIdFromToken(token);
            return ApiResponse.success(userService.getUserPermissions(userId));
        }
        return ApiResponse.success(List.of());
    }
}
