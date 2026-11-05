package com.clinic.dto.response;

import com.clinic.entity.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private LocalDateTime appointmentTime;
    private AppointmentStatus status;
    private String reason;
    private String patientName;
    private Long patientId;
    private LocalDateTime bookedAt;
    private boolean hasPrescription;   // lets the UI show "View prescription" vs "Write prescription"
    // Computed by the backend from CANCELLATION_CUTOFF_HOURS and the clinic clock, so the UI
    // never has to guess the rule. It describes what a PATIENT may do (doctors are not limited).
    private boolean patientCanChange;
    private int changeCutoffHours;
    // Seconds from "now" (clinic clock) until the start. Negative once it has started.
    // The UI counts down from this, so it never has to interpret a timezone.
    private long startsInSeconds;
}
