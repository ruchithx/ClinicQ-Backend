package com.dispensary.dms.queue.dto;

public record CallNextResponse(QueueEntryResponse calledEntry, QueueEntryRef completedEntry,
                               QueueSessionResponse session) {
}
