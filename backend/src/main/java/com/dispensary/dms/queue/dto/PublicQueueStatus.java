package com.dispensary.dms.queue.dto;

import com.dispensary.dms.queue.QueueSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The ONLY queue representation exposed without authentication (numbers only; no personal data). */
public record PublicQueueStatus(
        UUID id,
        String name,
        LocalDate sessionDate,
        QueueSessionStatus status,
        Integer currentServingNumber,
        List<Integer> servingNumbers,
        int waitingCount,
        List<Integer> waitingNumbers,
        int lastIssuedNumber,
        Instant updatedAt) {
}
