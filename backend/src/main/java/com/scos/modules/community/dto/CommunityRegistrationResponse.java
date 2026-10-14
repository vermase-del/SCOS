package com.scos.modules.community.dto;

import java.util.UUID;

public class CommunityRegistrationResponse {

    private UUID communityId;
    private String communityCode;
    private String communityName;
    private UUID adminUserId;
    private String adminUsername;
    private String token;
    private String tokenType = "Bearer";
    private int towersCreated;
    private int unitsCreated;
    private String message;

    public CommunityRegistrationResponse() {}

    public CommunityRegistrationResponse(UUID communityId, String communityCode, String communityName, UUID adminUserId, String adminUsername, String token, int towersCreated, int unitsCreated, String message) {
        this.communityId = communityId;
        this.communityCode = communityCode;
        this.communityName = communityName;
        this.adminUserId = adminUserId;
        this.adminUsername = adminUsername;
        this.token = token;
        this.tokenType = "Bearer";
        this.towersCreated = towersCreated;
        this.unitsCreated = unitsCreated;
        this.message = message;
    }

    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public String getCommunityCode() { return communityCode; }
    public void setCommunityCode(String communityCode) { this.communityCode = communityCode; }

    public String getCommunityName() { return communityName; }
    public void setCommunityName(String communityName) { this.communityName = communityName; }

    public UUID getAdminUserId() { return adminUserId; }
    public void setAdminUserId(UUID adminUserId) { this.adminUserId = adminUserId; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public int getTowersCreated() { return towersCreated; }
    public void setTowersCreated(int towersCreated) { this.towersCreated = towersCreated; }

    public int getUnitsCreated() { return unitsCreated; }
    public void setUnitsCreated(int unitsCreated) { this.unitsCreated = unitsCreated; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
