package com.dispensary.dms.queue.dto;

import com.dispensary.dms.queue.QueueEntryStatus;

import java.util.UUID;

public record QueueEntryRef(UUID id, int queueNumber, QueueEntryStatus status) {
}
