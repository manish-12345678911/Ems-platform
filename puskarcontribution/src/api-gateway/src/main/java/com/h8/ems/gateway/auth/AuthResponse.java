package com.h8.ems.gateway.auth;

import java.util.List;

public record AuthResponse(
        boolean success,
        String token,
        String username,
        String displayName,
        List<String> roles,
        String primaryRole,
        long expiresIn,
        String message
) {
    public static AuthResponse success(String token, String username, String displayName, List<String> roles, String primaryRole, long expiresIn) {
        return new AuthResponse(true, token, username, displayName, roles, primaryRole, expiresIn, "Authentication successful");
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(false, null, null, null, List.of(), null, 0, message);
    }
}
