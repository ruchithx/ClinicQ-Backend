package com.dispensary.dms.queue.dto;

import com.dispensary.dms.queue.QueueSessionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PublicQueueSessionSummary(
        UUID id,
        String name,
        LocalDate sessionDate,
        QueueSessionStatus status,
        Integer currentServingNumber,
        int waitingCount) {
}
