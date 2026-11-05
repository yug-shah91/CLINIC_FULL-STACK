package com.clinic.config;

import com.clinic.dto.request.AppointmentRequest;
import com.clinic.dto.request.RescheduleRequest;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockValidationTest {

    private LocalValidatorFactoryBean validator;
    private Clock fixedClinicClock;

    // Fixed clinic time: 2026-10-29 15:00:00 Asia/Kolkata
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 29, 15, 0, 0);

    @BeforeEach
    void setUp() {
        fixedClinicClock = Clock.fixed(FIXED_NOW.atZone(CLINIC_ZONE).toInstant(), CLINIC_ZONE);

        ClockConfig clockConfig = new ClockConfig();
        validator = clockConfig.validator(fixedClinicClock);
        validator.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (validator != null) {
            validator.destroy();
        }
    }

    @Test
    void futureConstraintRejectsPastTimeRelativeToClinicClock() {
        AppointmentRequest request = new AppointmentRequest();
        // 1 hour in the past according to clinic clock (14:00 vs 15:00)
        request.setAppointmentTime(FIXED_NOW.minusHours(1));
        request.setReason("Checkup");

        Set<ConstraintViolation<AppointmentRequest>> violations = validator.validate(request);

        boolean hasFutureViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("appointmentTime")
                        && v.getMessage().contains("future"));

        assertTrue(hasFutureViolation, "Must reject appointment time in the past of the clinic clock");
    }

    @Test
    void futureConstraintAcceptsFutureTimeRelativeToClinicClock() {
        AppointmentRequest request = new AppointmentRequest();
        // 1 hour in the future according to clinic clock (16:00 vs 15:00)
        request.setAppointmentTime(FIXED_NOW.plusHours(1));
        request.setReason("Checkup");

        Set<ConstraintViolation<AppointmentRequest>> violations = validator.validate(request);

        boolean hasFutureViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("appointmentTime"));

        assertFalse(hasFutureViolation, "Must accept appointment time in the future of the clinic clock");
    }

    @Test
    void rescheduleRequestRejectsPastTimeRelativeToClinicClock() {
        // 30 minutes in past relative to clinic clock
        RescheduleRequest request = new RescheduleRequest(FIXED_NOW.minusMinutes(30));

        Set<ConstraintViolation<RescheduleRequest>> violations = validator.validate(request);

        boolean hasFutureViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("appointmentTime"));

        assertTrue(hasFutureViolation, "Must reject reschedule time in the past of the clinic clock");
    }
}
