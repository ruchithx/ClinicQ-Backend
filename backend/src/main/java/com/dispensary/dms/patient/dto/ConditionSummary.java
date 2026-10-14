package com.dispensary.dms.patient.dto;

import java.time.Instant;

public record ConditionSummary(String conditionText, String icd10Code, Instant lastRecordedAt,
                               int occurrences) {
}
