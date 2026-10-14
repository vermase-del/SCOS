package com.scos.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UpdateRolePermissionsRequest {

    @NotNull(message = "Permission codes list is required")
    private List<String> permissionCodes;

    public UpdateRolePermissionsRequest() {}

    public UpdateRolePermissionsRequest(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }

    public List<String> getPermissionCodes() { return permissionCodes; }
    public void setPermissionCodes(List<String> permissionCodes) { this.permissionCodes = permissionCodes; }
}
