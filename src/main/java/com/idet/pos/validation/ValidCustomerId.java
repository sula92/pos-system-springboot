package com.idet.pos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = com.idet.pos.validation.CustomerIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCustomerId {

    String message() default "Customer ID must match C followed by 3 digits (e.g. C001)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
