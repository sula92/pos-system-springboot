package com.idet.pos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CustomerIdValidator implements ConstraintValidator<ValidCustomerId, String> {

    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^C\\d{3}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return CUSTOMER_ID_PATTERN.matcher(value).matches();
    }
}

