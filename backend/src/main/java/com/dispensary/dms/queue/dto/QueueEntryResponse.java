package com.dispensary.dms.queue.dto;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.queue.BookingChannel;
import com.dispensary.dms.queue.QueueEntryStatus;

import java.time.Instant;
import java.util.UUID;

public record QueueEntryResponse(
        UUID id,
        UUID queueSessionId,
        int queueNumber,
        QueueEntryStatus status,
        BookingChannel channel,
        String patientName,
        String contactPhone,
        String notes,
        Instant bookedAt,
        StaffRef bookedBy,
        Instant calledAt,
        StaffRef calledBy,
        Instant completedAt,
        Instant skippedAt,
        String skipReason,
        Instant cancelledAt,
        String cancelReason,
        int requeueCount,
        long version) {
}
