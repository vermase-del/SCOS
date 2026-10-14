package com.scos.modules.user.dto;

import java.util.UUID;

public class PermissionDto {

    private UUID id;
    private String code;
    private String module;
    private String description;

    public PermissionDto() {}

    public PermissionDto(UUID id, String code, String module, String description) {
        this.id = id;
        this.code = code;
        this.module = module;
        this.description = description;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
