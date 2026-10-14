package com.dispensary.dms.patient.dto;

import java.util.UUID;

public record PatientRef(UUID id, String patientNumber, String fullName) {
}
