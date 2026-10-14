package com.dispensary.dms.prescription;

import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.FieldError;
import com.dispensary.dms.medicine.MedicineEntity;
import com.dispensary.dms.medicine.MedicineRepository;
import com.dispensary.dms.prescription.dto.PrescriptionItemInput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resolves prescription item inputs into immutable item entities, snapshotting catalogue medicines
 * and collecting every item error before throwing a single 422 (spec section 10.3).
 */
@Component
public class PrescriptionItemFactory {

    private final MedicineRepository medicineRepository;

    public PrescriptionItemFactory(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public List<PrescriptionItemEntity> build(List<PrescriptionItemInput> inputs, String fieldPrefix) {
        List<PrescriptionItemEntity> items = new ArrayList<>(inputs.size());
        List<FieldError> errors = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            PrescriptionItemInput input = inputs.get(i);
            short line = (short) (i + 1);
            String name = input.medicineName();
            String strength = input.strength();
            var dosageForm = input.dosageForm();

            if (input.medicineId() != null) {
                MedicineEntity medicine = medicineRepository.findById(input.medicineId()).orElse(null);
                if (medicine == null) {
                    errors.add(new FieldError(fieldPrefix + "[" + i + "].medicineId", "NotFound",
                            "Medicine not found", input.medicineId().toString()));
                    continue;
                }
                if (!medicine.isActive()) {
                    errors.add(new FieldError(fieldPrefix + "[" + i + "].medicineId", "Inactive",
                            "Medicine is not available", input.medicineId().toString()));
                    continue;
                }
                name = medicine.getName();
                strength = medicine.getStrength();
                dosageForm = medicine.getDosageForm();
            }

            items.add(new PrescriptionItemEntity(UUID.randomUUID(), line, input.medicineId(), name,
                    strength, dosageForm, input.dose(), input.frequency(),
                    input.durationDays() == null ? null : input.durationDays().shortValue(),
                    input.quantity(), input.quantityUnit(), input.instructions()));
        }

        if (!errors.isEmpty()) {
            ErrorCode code = errors.stream().anyMatch(e -> e.code().equals("Inactive"))
                    && errors.stream().allMatch(e -> e.code().equals("Inactive"))
                    ? ErrorCode.MEDICINE_INACTIVE : ErrorCode.REFERENCED_RESOURCE_NOT_FOUND;
            throw new BusinessRuleException(code, "One or more prescription items are invalid", errors);
        }
        return items;
    }
}
