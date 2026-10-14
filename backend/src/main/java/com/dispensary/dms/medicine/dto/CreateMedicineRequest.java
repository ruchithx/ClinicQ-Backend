package com.dispensary.dms.medicine.dto;

import com.dispensary.dms.medicine.DosageForm;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMedicineRequest(
        @NotNull @Size(min = 1, max = 200) String name,
        @Size(max = 50) String strength,
        @NotNull DosageForm dosageForm) {
}
