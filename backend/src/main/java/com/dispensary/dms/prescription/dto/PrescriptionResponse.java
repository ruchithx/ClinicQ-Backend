package com.dispensary.dms.prescription.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.pharmacy.dto.Dispensing;
import com.dispensary.dms.prescription.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pharmacist/doctor view of a prescription (contains no diagnoses or clinical notes). */
public record PrescriptionResponse(
        UUID id,
        String prescriptionNumber,
        UUID consultationId,
        Instant consultationAt,
        PrescriptionStatus status,
        String notes,
        Instant submittedAt,
        StaffRef prescribedBy,
        PrescriptionPatient patient,
        List<PrescriptionItem> items,
        Instant startedAt,
        StaffRef startedBy,
        Instant cancelledAt,
        StaffRef cancelledBy,
        String cancelReason,
        UUID supersedesPrescriptionId,
        UUID supersededByPrescriptionId,
        Dispensing dispensing,
        long version) {
}
