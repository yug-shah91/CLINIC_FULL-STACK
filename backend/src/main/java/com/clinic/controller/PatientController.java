package com.clinic.controller;

import com.clinic.dto.request.MedicalNotesRequest;
import com.clinic.dto.request.PatientRequest;
import com.clinic.dto.response.PatientResponse;
import com.clinic.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient profile management")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    // PATIENT: Create their own profile after registration
    @PostMapping("/patients/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Create patient medical profile (patient only)")
    public ResponseEntity<PatientResponse> createProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.createProfile(email, request));
    }

    // PATIENT: View own profile
    @GetMapping("/patients/me")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Get my patient profile")
    public ResponseEntity<PatientResponse> getMyProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(patientService.getMyProfile(email));
    }

    // PATIENT: Update own profile
    @PutMapping("/patients/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update my patient profile")
    public ResponseEntity<PatientResponse> updateMyProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updateProfile(email, request));
    }

    // ADMIN: Get all patients (paginated)
    @GetMapping("/admin/patients")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all patients - Admin only")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(patientService.getAllPatients(safeSort(pageable)));
    }

    // ADMIN: Get specific patient by ID
    @GetMapping("/admin/patients/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get patient by ID - Admin only")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    // ADMIN: Search patients by name
    @PutMapping("/admin/patients/{id}/medical-notes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a patient's allergies and medical history - Admin only")
    public ResponseEntity<PatientResponse> updateMedicalNotes(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            @Valid @RequestBody MedicalNotesRequest request) {
        return ResponseEntity.ok(patientService.updateMedicalNotes(id, request, email));
    }

    @GetMapping("/admin/patients/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search patients by name or phone number - Admin only")
    public ResponseEntity<Page<PatientResponse>> searchPatients(
            @RequestParam String query,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(patientService.searchPatients(query, safeSort(pageable)));
    }

    private static Pageable safeSort(Pageable pageable) {
        return com.clinic.config.PageRequests.withAllowedSort(pageable,
                java.util.Set.of("name", "registeredAt"),
                org.springframework.data.domain.Sort.by("name"));
    }
}
