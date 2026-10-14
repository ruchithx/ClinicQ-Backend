package com.dispensary.dms.common.idempotency;

import java.util.UUID;

/**
 * Outcome of {@link IdempotencyService#begin}. When {@code replay} is true the caller must return
 * the stored resource with the stored status; otherwise it proceeds and calls
 * {@link IdempotencyService#complete}.
 */
public record IdempotencyResult(boolean replay, UUID rowId, UUID resourceId, int responseStatus) {

    public static IdempotencyResult proceed(UUID rowId) {
        return new IdempotencyResult(false, rowId, null, 0);
    }

    public static IdempotencyResult replay(UUID resourceId, int responseStatus) {
        return new IdempotencyResult(true, null, resourceId, responseStatus);
    }

    public boolean isReplay() {
        return replay;
    }
}
