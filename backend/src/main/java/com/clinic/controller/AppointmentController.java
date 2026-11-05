package com.clinic.controller;

import com.clinic.dto.request.AppointmentRequest;
import com.clinic.dto.request.RescheduleRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.DayAvailability;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment booking and management")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ───────────── Patient ─────────────

    @PostMapping("/appointments/book")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book appointment - Patient only")
    public ResponseEntity<AppointmentResponse> book(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(email, request));
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my appointments - Patient only")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(email));
    }

    // ───────────── Patient and doctor ─────────────

    @GetMapping("/appointments/availability")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Free and taken slots for the next days (default: 14 days from today)")
    public ResponseEntity<List<DayAvailability>> availability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(appointmentService.getAvailability(from, days));
    }

    @PutMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Cancel appointment (patients: only their own)")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, email, isAdmin(authentication)));
    }

    @PutMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Move an appointment to another slot (patients: only their own)")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            Authentication authentication,
            @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(
                id, email, isAdmin(authentication), request.appointmentTime()));
    }

    // ───────────── Doctor ─────────────

    @GetMapping("/admin/appointments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get appointments, optionally filtered by status and date range - Admin only")
    public ResponseEntity<Page<AppointmentResponse>> getAll(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "appointmentTime") Pageable pageable) {
        Pageable safe = com.clinic.config.PageRequests.withAllowedSort(pageable,
                java.util.Set.of("appointmentTime", "status", "bookedAt"),
                org.springframework.data.domain.Sort.by("appointmentTime"));
        return ResponseEntity.ok(appointmentService.getAllAppointments(status, from, to, safe));
    }

    @GetMapping("/admin/appointments/today")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get today's appointments - Admin only")
    public ResponseEntity<List<AppointmentResponse>> getToday() {
        return ResponseEntity.ok(appointmentService.getTodayAppointments());
    }

    @GetMapping("/admin/patients/{patientId}/appointments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get one patient's appointment history - Admin only")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(patientId));
    }

    @PutMapping("/admin/appointments/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm a booked appointment - Admin only")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    @PutMapping("/admin/appointments/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark appointment as completed - Admin only")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    // The JWT filter stores the role as "ROLE_ADMIN" / "ROLE_PATIENT" (see JwtFilter).
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
