package com.dispensary.dms.security;

import com.dispensary.dms.user.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal resolved from the access-token JWT and passed explicitly from controllers
 * to services (spec section 9.4). Services never read the security context directly.
 */
public record CurrentUser(UUID id, String username, Set<Role> roles, boolean mustChangePassword) {

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isDoctor() {
        return roles.contains(Role.DOCTOR);
    }

    public boolean isPharmacist() {
        return roles.contains(Role.PHARMACIST);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }
}
