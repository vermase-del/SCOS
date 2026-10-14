package com.scos.modules.auth.dto;

import com.scos.modules.user.dto.UserDto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private UserDto user;

    public AuthResponse() {}

    public AuthResponse(String token, String tokenType, UserDto user) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
