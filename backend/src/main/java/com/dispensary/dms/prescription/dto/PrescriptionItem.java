package com.dispensary.dms.prescription.dto;

import com.dispensary.dms.medicine.DosageForm;

import java.math.BigDecimal;
import java.util.UUID;

public record PrescriptionItem(
        UUID id,
        int lineNumber,
        UUID medicineId,
        String medicineName,
        String strength,
        DosageForm dosageForm,
        String dose,
        String frequency,
        Integer durationDays,
        BigDecimal quantity,
        String quantityUnit,
        String instructions) {
}
