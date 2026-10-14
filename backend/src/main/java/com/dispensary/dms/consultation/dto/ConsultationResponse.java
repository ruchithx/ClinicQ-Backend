package com.dispensary.dms.consultation.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.patient.dto.PatientRef;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        PatientRef patient,
        StaffRef doctor,
        UUID queueEntryId,
        Instant consultationAt,
        String presentingComplaint,
        String clinicalNotes,
        List<Diagnosis> diagnoses,
        List<PrescriptionResponse> prescriptions,
        Instant createdAt) {
}
