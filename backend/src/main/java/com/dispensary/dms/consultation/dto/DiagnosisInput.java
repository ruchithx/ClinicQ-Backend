package com.dispensary.dms.consultation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DiagnosisInput(
        @NotNull @Size(min = 1, max = 500) String conditionText,
        @Pattern(regexp = "^[A-Z][0-9]{2}(\\.[0-9A-Z]{1,4})?$") String icd10Code) {
}
