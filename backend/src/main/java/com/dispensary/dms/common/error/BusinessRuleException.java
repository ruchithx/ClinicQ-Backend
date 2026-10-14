package com.dispensary.dms.common.error;

import java.util.List;
import java.util.Map;

/** 422-style convenience subclass for business-rule violations. */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(ErrorCode code, String detail) {
        super(code, detail, Map.of(), List.of());
    }

    public BusinessRuleException(ErrorCode code, String detail, List<FieldError> errors) {
        super(code, detail, Map.of(), errors);
    }

    public BusinessRuleException(ErrorCode code, String detail, Map<String, Object> context,
                                 List<FieldError> errors) {
        super(code, detail, context, errors);
    }
}
