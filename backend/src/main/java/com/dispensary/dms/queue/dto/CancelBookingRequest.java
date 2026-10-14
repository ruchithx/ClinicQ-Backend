package com.dispensary.dms.queue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(@NotNull @Size(min = 3, max = 500) String reason) {
}
