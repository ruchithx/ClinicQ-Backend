package com.dispensary.dms.pharmacy.dto;

import com.dispensary.dms.pharmacy.DispensedItemStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DispensingItem(UUID prescriptionItemId, BigDecimal quantityDispensed,
                             DispensedItemStatus status, String note) {
}
