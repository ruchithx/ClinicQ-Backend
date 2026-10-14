package com.dispensary.dms.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Field-level problem. {@code rejectedValue} is omitted for passwords and free-text clinical fields.
 */
public record FieldError(String field, String code, String message,
                         @JsonInclude(JsonInclude.Include.NON_NULL) Object rejectedValue) {

    public FieldError(String field, String code, String message) {
        this(field, code, message, null);
    }
}
