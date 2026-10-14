package com.dispensary.dms.common.validation;

import com.dispensary.dms.prescription.dto.PrescriptionItemInput;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MedicineReferenceValidator
        implements ConstraintValidator<MedicineReference, PrescriptionItemInput> {

    @Override
    public boolean isValid(PrescriptionItemInput item, ConstraintValidatorContext context) {
        if (item == null) {
            return true;
        }
        boolean hasId = item.medicineId() != null;
        boolean hasName = item.medicineName() != null && !item.medicineName().isBlank();

        if (hasId) {
            // medicineId supplied: name/strength/dosageForm must be absent
            boolean extras = hasName || item.strength() != null || item.dosageForm() != null;
            if (extras) {
                return violation(context, "medicineName",
                        "Do not send medicineName/strength/dosageForm together with medicineId");
            }
            return true;
        }
        // no medicineId: medicineName is required (free-text item)
        if (!hasName) {
            return violation(context, "medicineName", "medicineName is required when medicineId is absent");
        }
        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
