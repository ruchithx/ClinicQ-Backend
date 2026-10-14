package com.dispensary.dms.queue.dto;

public record CloseQueueSessionRequest(Boolean force) {

    public boolean forceOrDefault() {
        return Boolean.TRUE.equals(force);
    }
}
