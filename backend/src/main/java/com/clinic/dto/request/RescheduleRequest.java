package com.clinic.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull(message = "New appointment time is required")
        @Future(message = "Appointment must be scheduled in the future")
        LocalDateTime appointmentTime) {}
