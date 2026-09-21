package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.domain.User;

import java.time.Instant;

/** El DTO publico del usuario. Nunca lleva password, ni siquiera codificada. */
public record UserResponse(Long id,
                           String email,
                           String displayName,
                           String role,
                           Instant createdAt) {

    public static UserResponse de(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getCreatedAt());
    }
}
