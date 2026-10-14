package com.dispensary.dms.pharmacy.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.pharmacy.DispensingOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Dispensing(UUID id, StaffRef pharmacist, Instant dispensedAt, DispensingOutcome outcome,
                         String notes, List<DispensingItem> items) {
}
