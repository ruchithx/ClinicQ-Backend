package com.dispensary.dms.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AddPrescriptionRequest(
        UUID supersedesPrescriptionId,
        @Size(max = 1000) String notes,
        @NotNull @Size(min = 1, max = 30) @Valid List<PrescriptionItemInput> items) {
}
