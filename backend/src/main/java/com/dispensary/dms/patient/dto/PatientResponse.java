package com.dispensary.dms.patient.dto;

import com.dispensary.dms.patient.Sex;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String patientNumber,
        String fullName,
        Sex sex,
        LocalDate dateOfBirth,
        boolean dateOfBirthEstimated,
        int ageYears,
        String phoneNumber,
        String address,
        String knownAllergies,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
