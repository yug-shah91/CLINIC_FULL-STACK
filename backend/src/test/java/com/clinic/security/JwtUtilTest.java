package com.clinic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void rejectsNullOrBlankSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("   ", 3600000));
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        // 31 characters = 31 bytes
        String shortSecret = "a".repeat(31);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtUtil(shortSecret, 3600000));
        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }

    @Test
    void rejectsNonPositiveExpiration() {
        String validSecret = "this-is-a-valid-secure-secret-key-of-32-bytes!!";
        assertThrows(IllegalStateException.class, () -> new JwtUtil(validSecret, 0));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(validSecret, -1));
    }

    @Test
    void acceptsSecretWith32OrMoreBytesAndGeneratesValidToken() {
        String validSecret = "this-is-a-valid-secure-secret-key-of-32-bytes!!";
        JwtUtil jwtUtil = new JwtUtil(validSecret, 3600000);

        String token = jwtUtil.generateToken("patient@clinic.com", "PATIENT");
        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("patient@clinic.com", jwtUtil.extractEmail(token));
        assertEquals("PATIENT", jwtUtil.extractRole(token));
    }
}
