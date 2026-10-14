package com.dispensary.dms.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint: exactly one of the named record components must be non-null.
 */
@Documented
@Constraint(validatedBy = ExactlyOneOfValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface ExactlyOneOf {
    String[] value();

    String message() default "Exactly one of the mutually exclusive fields must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
