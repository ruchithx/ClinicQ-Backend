package com.dispensary.dms.prescription.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.patient.dto.PatientRef;
import com.dispensary.dms.pharmacy.DispensingOutcome;
import com.dispensary.dms.prescription.PrescriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionSummary(
        UUID id,
        String prescriptionNumber,
        UUID consultationId,
        PrescriptionStatus status,
        PatientRef patient,
        StaffRef prescribedBy,
        Instant submittedAt,
        int itemCount,
        StaffRef startedBy,
        DispensingOutcome dispensingOutcome,
        Instant dispensedAt) {
}
