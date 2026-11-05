package com.clinic.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAccessDeniedReturns403WithFixedMessage() {
        AccessDeniedException ex = new AccessDeniedException("Access denied: This prescription does not belong to you.");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("You do not have permission to perform this action", response.getBody().get("error"));
    }

    @Test
    void handleDuplicateResourceReturns409Conflict() {
        DuplicateResourceException ex = new DuplicateResourceException("Patient profile already exists for this user");
        ResponseEntity<Map<String, Object>> response = handler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Patient profile already exists for this user", response.getBody().get("error"));
    }

    @Test
    void handleSlotConflictReturns409Conflict() {
        SlotConflictException ex = new SlotConflictException("Time slot is already booked.");
        ResponseEntity<Map<String, Object>> response = handler.handleSlotConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    void handleInvalidStateTransitionReturns400BadRequest() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException(
                "Appointments cannot be cancelled less than 2 hours before scheduled time.");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidStateTransition(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Appointments cannot be cancelled less than 2 hours before scheduled time.",
                response.getBody().get("error"));
    }

    @Test
    void handleRateLimitReturns429TooManyRequests() {
        RateLimitExceededException ex = new RateLimitExceededException("Too many attempts");
        ResponseEntity<Map<String, Object>> response = handler.handleRateLimit(ex);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(429, response.getBody().get("status"));
    }
}
