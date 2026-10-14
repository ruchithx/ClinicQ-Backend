package com.dispensary.dms.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotNull @Size(min = 1, max = 128) String currentPassword,
        @NotNull @Size(min = 12, max = 128) String newPassword) {
}
