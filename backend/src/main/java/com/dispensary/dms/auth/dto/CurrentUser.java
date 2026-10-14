package com.dispensary.dms.auth.dto;

import com.dispensary.dms.user.Role;

import java.util.Set;
import java.util.UUID;

/** Response schema {@code CurrentUser} (GET /auth/me and nested in TokenResponse). */
public record CurrentUser(
        UUID id,
        String username,
        String fullName,
        Set<Role> roles,
        boolean mustChangePassword,
        String professionalRegistrationNumber) {
}
