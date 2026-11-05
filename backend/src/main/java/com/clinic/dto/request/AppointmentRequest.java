package com.clinic.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequest {

    // @Future — appointment time must be in the future (cannot book past slots)
    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment must be scheduled in the future")
    private LocalDateTime appointmentTime;

    @Size(max = 300, message = "Reason cannot exceed 300 characters")
    private String reason;
}
