package com.clinic.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a password satisfies length and UTF-8 byte limits.
 * Standard BCrypt algorithms truncate passwords at 72 bytes, which
 * differs from character length when non-ASCII / multi-byte characters are used.
 */
@Documented
@Constraint(validatedBy = BcryptPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface BcryptPassword {

    String message() default "Password must not exceed 72 bytes in UTF-8 encoding";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 8;

    int maxBytes() default 72;
}
