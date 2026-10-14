package com.dispensary.dms.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DispenseRequest(
        @Size(max = 1000) String notes,
        @NotNull @Size(min = 1, max = 30) @Valid List<DispenseItemInput> items) {
}
