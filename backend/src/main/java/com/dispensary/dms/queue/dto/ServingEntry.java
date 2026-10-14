package com.dispensary.dms.queue.dto;

import com.dispensary.dms.common.dto.StaffRef;

import java.time.Instant;
import java.util.UUID;

public record ServingEntry(UUID id, int queueNumber, String patientName, Instant calledAt,
                           StaffRef calledBy) {
}
