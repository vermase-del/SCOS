package com.scos.modules.user.dto;

import java.util.List;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private boolean active;
    private boolean verified;
    private List<String> roles;
    private String primaryRole;
    private List<String> permissions;
    private UUID activeCommunityId;

    public UserDto() {}

    public UserDto(UUID id, String username, String email, String phoneNumber, String firstName, String lastName, String avatarUrl, boolean active, boolean verified, List<String> roles, UUID activeCommunityId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.active = active;
        this.verified = verified;
        this.roles = roles;
        this.primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "RESIDENT";
        this.activeCommunityId = activeCommunityId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) {
        this.roles = roles;
        if (roles != null && !roles.isEmpty() && this.primaryRole == null) {
            this.primaryRole = roles.get(0);
        }
    }

    public String getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(String primaryRole) { this.primaryRole = primaryRole; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public UUID getActiveCommunityId() { return activeCommunityId; }
    public void setActiveCommunityId(UUID activeCommunityId) { this.activeCommunityId = activeCommunityId; }
}
