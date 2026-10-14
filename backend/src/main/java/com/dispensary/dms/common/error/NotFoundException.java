package com.dispensary.dms.common.error;

import java.util.List;
import java.util.Map;

/** 404-style convenience subclass. */
public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode code, String detail) {
        super(code, detail, Map.of(), List.of());
    }
}
