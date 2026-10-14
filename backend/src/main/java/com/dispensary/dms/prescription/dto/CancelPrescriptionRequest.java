package com.dispensary.dms.prescription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelPrescriptionRequest(
        @NotNull @Size(min = 3, max = 500) String reason) {
}
