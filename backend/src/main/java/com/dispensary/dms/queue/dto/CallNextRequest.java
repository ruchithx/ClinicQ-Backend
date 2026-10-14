package com.dispensary.dms.queue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record CallNextRequest(
        @Min(1) @Max(999) Integer expectedLastCalledNumber,
        UUID completeCurrentEntryId) {
}
