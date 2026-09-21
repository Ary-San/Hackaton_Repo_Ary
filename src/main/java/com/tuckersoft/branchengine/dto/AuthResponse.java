package com.tuckersoft.branchengine.dto;

public record AuthResponse(String token,
                           String type,
                           String email,
                           String displayName,
                           String role) {

    public static AuthResponse bearer(String token, String email, String displayName, String role) {
        return new AuthResponse(token, "Bearer", email, displayName, role);
    }
}
