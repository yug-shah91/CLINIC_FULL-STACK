package com.clinic.dto.request;

import jakarta.validation.constraints.Size;

/** Used by the doctor to update a patient's medical record. */
public record MedicalNotesRequest(
        @Size(max = 1000, message = "Allergies cannot exceed 1000 characters")
        String allergies,
        @Size(max = 2000, message = "Medical history cannot exceed 2000 characters")
        String medicalHistory) {}
