package com.scos.modules.user.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "scos_permissions")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 40)
    private String module;

    @Column(nullable = false, length = 200)
    private String description;

    public PermissionEntity() {}

    public PermissionEntity(UUID id, String code, String module, String description) {
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
