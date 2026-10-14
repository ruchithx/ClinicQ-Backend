package com.dispensary.dms.user.dto;

import com.dispensary.dms.user.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$") String username,
        @NotNull @Size(min = 1, max = 150) String fullName,
        @NotEmpty @Size(max = 3) Set<Role> roles,
        @Size(max = 50) String professionalRegistrationNumber,
        @NotNull @Size(min = 12, max = 128) String temporaryPassword) {
}
