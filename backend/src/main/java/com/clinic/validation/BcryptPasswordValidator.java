package com.clinic.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class BcryptPasswordValidator implements ConstraintValidator<BcryptPassword, String> {

    private int minLength;
    private int maxBytes;

    @Override
    public void initialize(BcryptPassword constraint) {
        this.minLength = constraint.minLength();
        this.maxBytes = constraint.maxBytes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Use @NotBlank to handle null or empty values
        }

        if (value.length() < minLength) {
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Password must be at least " + minLength + " characters")
                        .addConstraintViolation();
            }
            return false;
        }

        byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length > maxBytes) {
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Password exceeds " + maxBytes + " bytes in UTF-8 (" + utf8Bytes.length
                                + " bytes). Non-ASCII characters use multiple bytes.")
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }
}
