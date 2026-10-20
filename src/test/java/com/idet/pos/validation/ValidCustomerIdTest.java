package com.idet.pos.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidCustomerIdTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validCustomerId_passesValidation() {
        ValidationSample sample = new ValidationSample("C001");

        assertEquals(0, validator.validate(sample).size());
    }

    @Test
    void invalidCustomerId_failsValidation() {
        ValidationSample sample = new ValidationSample("BAD");

        assertEquals(1, validator.validate(sample).size());
    }

    private static class ValidationSample {
        @ValidCustomerId
        private final String id;

        private ValidationSample(String id) {
            this.id = id;
        }
    }
}

