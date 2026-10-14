package com.dispensary.dms.common.error;

import java.util.List;
import java.util.Map;

/** 409-style convenience subclass. */
public class ConflictException extends ApiException {
    public ConflictException(ErrorCode code, String detail) {
        super(code, detail, Map.of(), List.of());
    }

    public ConflictException(ErrorCode code, String detail, Map<String, Object> context) {
        super(code, detail, context, List.of());
    }
}
