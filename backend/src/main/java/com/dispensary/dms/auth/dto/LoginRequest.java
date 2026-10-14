package com.dispensary.dms.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull @Size(min = 1, max = 50) String username,
        @NotNull @Size(min = 1, max = 128) String password) {
}
