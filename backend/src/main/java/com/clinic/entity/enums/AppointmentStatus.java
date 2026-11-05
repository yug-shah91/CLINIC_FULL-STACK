package com.clinic.entity.enums;

public enum AppointmentStatus {
    SCHEDULED,   // Booked by the patient, waiting for the doctor to confirm (shown as "Booked" in the UI)
    CONFIRMED,   // Doctor has accepted the booking
    COMPLETED,   // Doctor marked as done, so a prescription can now be written
    CANCELLED    // Cancelled by the patient or the doctor
}
