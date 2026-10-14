package com.dispensary.dms.prescription.dto;

import com.dispensary.dms.common.validation.MedicineReference;
import com.dispensary.dms.medicine.DosageForm;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Either medicineId (catalogue) or medicineName (free text), never both. */
@MedicineReference
public record PrescriptionItemInput(
        UUID medicineId,
        @Size(min = 1, max = 200) String medicineName,
        @Size(max = 50) String strength,
        DosageForm dosageForm,
        @NotNull @Size(min = 1, max = 100) String dose,
        @NotNull @Size(min = 1, max = 100) String frequency,
        @Min(1) @Max(365) Integer durationDays,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("99999.99")
        @Digits(integer = 5, fraction = 2) BigDecimal quantity,
        @NotNull @Size(min = 1, max = 30) String quantityUnit,
        @Size(max = 500) String instructions) {
}
