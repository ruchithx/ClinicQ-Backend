package com.dispensary.dms.queue.dto;

import jakarta.validation.constraints.Size;

public record SkipQueueEntryRequest(@Size(max = 300) String reason) {
}
