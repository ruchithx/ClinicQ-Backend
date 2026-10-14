package com.dispensary.dms.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotNull @Size(min = 12, max = 128) String temporaryPassword) {
}
