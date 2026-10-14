package com.dispensary.dms.common.idempotency;

/**
 * Carries a response body together with the HTTP status that should be sent, so a controller can
 * replay an idempotent create with the original status (201 on first, 201/200 on replay).
 */
public final class IdempotentHelper {

    private IdempotentHelper() {
    }

    public record Result<T>(T body, int status) {
    }
}
