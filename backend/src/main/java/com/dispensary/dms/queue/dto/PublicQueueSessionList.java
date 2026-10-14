package com.dispensary.dms.queue.dto;

import java.time.LocalDate;
import java.util.List;

public record PublicQueueSessionList(LocalDate date, List<PublicQueueSessionSummary> items) {
}
