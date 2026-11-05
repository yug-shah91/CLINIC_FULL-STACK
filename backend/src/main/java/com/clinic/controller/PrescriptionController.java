package com.clinic.controller;

import com.clinic.dto.request.PrescriptionRequest;
import com.clinic.dto.response.PrescriptionResponse;
import com.clinic.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management")
@SecurityRequirement(name = "bearerAuth")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // ADMIN: Write a prescription for a completed appointment
    @PostMapping("/admin/prescriptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Write prescription - Admin only")
    public ResponseEntity<PrescriptionResponse> write(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.writePrescription(request, email));
    }

    // ADMIN: Get patient's full prescription history
    @GetMapping("/admin/patients/{patientId}/prescriptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get patient's prescription history - Admin only")
    public ResponseEntity<List<PrescriptionResponse>> getHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(prescriptionService.getPatientHistory(patientId));
    }

    // PATIENT: View their own prescription by appointment ID
    // Lets the doctor open an existing prescription instead of writing a second one
    @GetMapping("/admin/appointments/{appointmentId}/prescription")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get the prescription of an appointment - Admin only")
    public ResponseEntity<PrescriptionResponse> getForAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionByAppointment(appointmentId));
    }

    @GetMapping("/prescriptions/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my prescription by appointment - Patient only")
    public ResponseEntity<PrescriptionResponse> getMyPrescription(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(prescriptionService.getMyPrescription(appointmentId, email));
    }
}
