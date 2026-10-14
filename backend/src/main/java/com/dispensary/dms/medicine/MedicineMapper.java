package com.dispensary.dms.medicine;

import com.dispensary.dms.medicine.dto.MedicineResponse;
import org.springframework.stereotype.Component;

@Component
public class MedicineMapper {

    public MedicineResponse toResponse(MedicineEntity m) {
        return new MedicineResponse(m.getId(), m.getName(), m.getStrength(), m.getDosageForm(),
                m.isActive(), m.getCreatedAt(), m.getUpdatedAt(), m.getVersion());
    }
}
