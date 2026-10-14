package com.scos.modules.user.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "scos_roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private boolean systemRole = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "scos_role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    public RoleEntity() {}

    public RoleEntity(UUID id, String code, String name, String description, boolean systemRole) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
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

    public Set<PermissionEntity> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionEntity> permissions) { this.permissions = permissions; }
}
