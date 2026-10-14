package com.dispensary.dms.user.dto;

import com.dispensary.dms.user.Role;

import java.util.Set;
import java.util.UUID;

public record StaffMember(UUID id, String fullName, Set<Role> roles) {
}
