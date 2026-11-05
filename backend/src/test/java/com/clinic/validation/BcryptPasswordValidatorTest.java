package com.clinic.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordValidatorTest {

    private BcryptPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BcryptPasswordValidator();
        BcryptPassword annotation = new BcryptPassword() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return BcryptPassword.class;
            }

            @Override
            public String message() {
                return "Password must not exceed 72 bytes";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() {
                return (Class<? extends Payload>[]) new Class<?>[0];
            }

            @Override
            public int minLength() {
                return 8;
            }

            @Override
            public int maxBytes() {
                return 72;
            }
        };
        validator.initialize(annotation);
    }

    @Test
    void acceptsValidAsciiPassword() {
        // When valid, context is not touched
        assertTrue(validator.isValid("ValidPass123", null));
    }

    @Test
    void acceptsBoundary72BytePassword() {
        String exact72 = "A1" + "a".repeat(70);
        assertTrue(validator.isValid(exact72, null));
    }

    @Test
    void rejectsTooShortPassword() {
        // Custom violation builder only called if context is present
        assertFalse(validator.isValid("short1", null));
    }

    @Test
    void rejectsPasswordExceeding72Utf8Bytes() {
        // 73 ASCII characters = 73 bytes
        String tooLong = "A1" + "a".repeat(71);
        assertFalse(validator.isValid(tooLong, null));
    }

    @Test
    void rejectsMultiByteNonAsciiPasswordExceeding72Bytes() {
        // "Password123" (11 bytes) + 16 emojis * 4 bytes (64 bytes) = 75 bytes > 72 bytes
        String nonAscii = "Password123" + "🔒".repeat(16);
        assertFalse(validator.isValid(nonAscii, null));
    }
}
