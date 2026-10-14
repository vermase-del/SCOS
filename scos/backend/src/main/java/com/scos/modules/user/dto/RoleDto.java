package com.scos.modules.user.dto;

import java.util.List;
import java.util.UUID;

public class RoleDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean systemRole;
    private List<String> permissions;

    public RoleDto() {}

    public RoleDto(UUID id, String code, String name, String description, boolean systemRole, List<String> permissions) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.permissions = permissions;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
