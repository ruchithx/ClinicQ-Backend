package com.dispensary.dms.consultation.dto;

import com.dispensary.dms.prescription.dto.PrescriptionInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateConsultationRequest(
        @NotNull UUID patientId,
        UUID queueEntryId,
        @Size(max = 1000) String presentingComplaint,
        @Size(max = 5000) String clinicalNotes,
        @NotNull @Size(min = 1, max = 10) @Valid List<DiagnosisInput> diagnoses,
        @Valid PrescriptionInput prescription) {
}
