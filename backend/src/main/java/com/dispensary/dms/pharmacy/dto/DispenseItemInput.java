package com.dispensary.dms.pharmacy.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DispenseItemInput(
        @NotNull UUID prescriptionItemId,
        @NotNull @DecimalMin("0") @DecimalMax("99999.99") @Digits(integer = 5, fraction = 2)
        BigDecimal quantityDispensed,
        @Size(max = 500) String note) {
}
