package com.dispensary.dms.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint on a prescription item input: either {@code medicineId} (catalogue) or
 * {@code medicineName} (free text), never both. When {@code medicineId} is present, name/strength/
 * dosageForm must be absent (openapi if/then).
 */
@Documented
@Constraint(validatedBy = MedicineReferenceValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface MedicineReference {
    String message() default "Provide either medicineId or medicineName, never both";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
