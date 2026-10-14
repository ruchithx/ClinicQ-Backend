package com.dispensary.dms.consultation.dto;

import java.util.UUID;

public record Diagnosis(UUID id, int lineNumber, String conditionText, String icd10Code) {
}
