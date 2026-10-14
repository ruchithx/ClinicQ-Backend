package com.dispensary.dms.user.dto;

import com.dispensary.dms.user.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        Set<Role> roles,
        boolean enabled,
        boolean mustChangePassword,
        String professionalRegistrationNumber,
        Instant lastLoginAt,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
