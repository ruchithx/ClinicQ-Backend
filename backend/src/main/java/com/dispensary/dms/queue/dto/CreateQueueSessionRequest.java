package com.dispensary.dms.queue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateQueueSessionRequest(
        @NotNull LocalDate sessionDate,
        @NotNull @Size(min = 1, max = 100) String name,
        UUID assignedDoctorId) {
}
