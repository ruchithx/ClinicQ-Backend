package com.dispensary.dms.queue.dto;

public record QueueCounts(int waiting, int serving, int completed, int skipped, int cancelled) {
}
