package com.dispensary.dms.prescription.dto;

import com.dispensary.dms.patient.Sex;

import java.util.UUID;

/** Minimum patient data needed to dispense safely (no address, no DOB). */
public record PrescriptionPatient(
        UUID id,
        String patientNumber,
        String fullName,
        Sex sex,
        int ageYears,
        boolean dateOfBirthEstimated,
        String knownAllergies) {
}
