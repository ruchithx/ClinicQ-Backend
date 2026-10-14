package com.dispensary.dms.queue.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.queue.QueueSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QueueSessionResponse(
        UUID id,
        LocalDate sessionDate,
        String name,
        QueueSessionStatus status,
        StaffRef assignedDoctor,
        int lastIssuedNumber,
        Integer currentServingNumber,
        Integer nextWaitingNumber,
        QueueCounts counts,
        List<ServingEntry> servingEntries,
        Instant openedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
