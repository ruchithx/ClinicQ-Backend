package com.dispensary.dms.common.error;

import java.util.List;
import java.util.Map;

/**
 * Base application exception carrying an {@link ErrorCode}, a human-readable detail, optional
 * context fields (serialised as Problem properties) and optional field errors.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCode code;
    private final transient Map<String, Object> context;
    private final transient List<FieldError> errors;

    public ApiException(ErrorCode code, String detail, Map<String, Object> context, List<FieldError> errors) {
        super(detail);
        this.code = code;
        this.context = context == null ? Map.of() : context;
        this.errors = errors == null ? List.of() : errors;
    }

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, Map.of(), List.of());
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> context() {
        return context;
    }

    public List<FieldError> errors() {
        return errors;
    }

    public static ApiException of(ErrorCode code, String detail) {
        return new ApiException(code, detail);
    }
}
