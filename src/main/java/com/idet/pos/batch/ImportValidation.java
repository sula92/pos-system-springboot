package com.idet.pos.batch;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates an imported CSV row against the same Jakarta constraints the REST API uses.
 * A failed row throws a skippable {@link ValidationException}.
 */
final class ImportValidation {

    private ImportValidation() {
    }

    static <T> void validate(Validator validator, T bean, String rowKey) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new ValidationException("Invalid row " + rowKey + " - " + details);
        }
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
