package com.dispensary.dms.medicine.dto;

import com.dispensary.dms.medicine.DosageForm;

import java.time.Instant;
import java.util.UUID;

public record MedicineResponse(
        UUID id,
        String name,
        String strength,
        DosageForm dosageForm,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
