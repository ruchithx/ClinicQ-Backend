package com.dispensary.dms.patient.dto;

import com.dispensary.dms.common.validation.ExactlyOneOf;
import com.dispensary.dms.common.validation.PatientNumberDeserializer;
import com.dispensary.dms.common.validation.PhoneNumberDeserializer;
import com.dispensary.dms.patient.Sex;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@ExactlyOneOf({"dateOfBirth", "estimatedAgeYears"})
public record CreatePatientRequest(
        @NotNull
        @JsonDeserialize(using = PatientNumberDeserializer.class)
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{2,19}$") String patientNumber,

        @NotNull @Size(min = 1, max = 150) String fullName,
        @NotNull Sex sex,
        LocalDate dateOfBirth,
        @Min(0) @Max(130) Integer estimatedAgeYears,

        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phoneNumber,

        @Size(max = 300) String address,
        @Size(max = 1000) String knownAllergies) {
}
