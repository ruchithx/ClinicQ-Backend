package com.dispensary.dms.user.dto;

import com.dispensary.dms.user.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @NotNull @Size(min = 1, max = 150) String fullName,
        @NotEmpty @Size(max = 3) Set<Role> roles,
        @NotNull Boolean enabled,
        @Size(max = 50) String professionalRegistrationNumber,
        @NotNull @PositiveOrZero Long version) {
}
