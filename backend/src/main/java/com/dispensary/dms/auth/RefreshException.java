package com.dispensary.dms.auth;

import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * Refresh-flow failure that also signals whether the browser's refresh cookie should be cleared.
 */
public class RefreshException extends ApiException {

    private final boolean clearCookie;

    public RefreshException(ErrorCode code, String detail, boolean clearCookie) {
        super(code, detail, Map.of(), List.of());
        this.clearCookie = clearCookie;
    }

    public boolean shouldClearCookie() {
        return clearCookie;
    }
}
