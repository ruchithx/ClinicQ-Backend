package com.dispensary.dms.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class ExactlyOneOfValidator implements ConstraintValidator<ExactlyOneOf, Object> {

    private String[] fields;

    @Override
    public void initialize(ExactlyOneOf constraintAnnotation) {
        this.fields = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        int present = 0;
        for (String field : fields) {
            if (wrapper.getPropertyValue(field) != null) {
                present++;
            }
        }
        if (present == 1) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(fields[0])
                .addConstraintViolation();
        return false;
    }
}
