package com.clinic.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PrescriptionRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 500, message = "Diagnosis cannot exceed 500 characters")
    private String diagnosis;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @FutureOrPresent(message = "Follow-up date cannot be in the past")
    private LocalDate followUpDate;

    // A prescription without medicines is incomplete. @Valid makes Spring check every
    // medicine in the list too (without it the rules on MedicineRequest would be ignored).
    @NotEmpty(message = "Add at least one medicine")
    @Size(max = 20, message = "A prescription can contain at most 20 medicines")
    @Valid
    private List<MedicineRequest> medicines;

    @Data
    public static class MedicineRequest {

        @NotBlank(message = "Medicine name is required")
        @Size(max = 200, message = "Medicine name cannot exceed 200 characters")
        private String name;

        @NotBlank(message = "Dosage is required")
        @Size(max = 50, message = "Dosage cannot exceed 50 characters")
        private String dosage;

        @NotBlank(message = "Frequency is required")
        @Size(max = 100, message = "Frequency cannot exceed 100 characters")
        private String frequency;

        @NotNull(message = "Duration in days is required")
        @Min(value = 1, message = "Duration must be at least 1 day")
        @Max(value = 365, message = "Duration cannot be more than 365 days")
        private Integer durationDays;

        @Size(max = 300, message = "Instructions cannot exceed 300 characters")
        private String instructions;
    }
}
