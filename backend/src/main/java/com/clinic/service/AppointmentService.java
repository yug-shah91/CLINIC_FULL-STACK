package com.clinic.service;

import com.clinic.dto.request.AppointmentRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.DayAvailability;
import com.clinic.entity.Appointment;
import com.clinic.entity.Patient;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.exception.InvalidStateTransitionException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.exception.SlotConflictException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final Clock clock;

    @Value("${clinic.booking-horizon-days:60}")
    private int bookingHorizonDays;

    @Value("${clinic.cancellation-cutoff-hours:2}")
    private int cancellationCutoffHours;

    private static final List<AppointmentStatus> ACTIVE =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private LocalDate today() { return LocalDate.now(clock); }

    // ───────────────────────── Booking ─────────────────────────

    @Transactional
    public AppointmentResponse bookAppointment(String patientEmail, AppointmentRequest request) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient profile not found. Please create your profile first."));

        LocalDateTime requestedTime = request.getAppointmentTime();
        SlotRules.validateBookable(requestedTime, now(), bookingHorizonDays);
        checkSlotFree(requestedTime);
        checkOnePerDay(patient.getId(), 0L, requestedTime);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .appointmentTime(requestedTime)
                .reason(request.getReason())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        return toResponse(appointmentRepository.save(appointment));
    }

    private void checkOnePerDay(Long patientId, Long excludeAppointmentId, LocalDateTime time) {
        LocalDate day = time.toLocalDate();
        if (appointmentRepository.existsByPatientIdAndIdNotAndAppointmentTimeBetweenAndStatusIn(
                patientId, excludeAppointmentId, day.atStartOfDay(), day.atTime(LocalTime.MAX), ACTIVE)) {
            throw new SlotConflictException(
                    "You already have an appointment on " + day + ". Please cancel or reschedule it first.");
        }
    }

    private void checkSlotFree(LocalDateTime start) {
        if (appointmentRepository.existsByAppointmentTimeAndStatusNot(start, AppointmentStatus.CANCELLED)) {
            throw new SlotConflictException(
                    "Time slot " + start + " is already booked. Please choose another time.");
        }
    }

    /** Free / taken slots for the next `days` days, starting at `from` (default: today). */
    @Transactional(readOnly = true)
    public List<DayAvailability> getAvailability(LocalDate from, int days) {
        if (days < 1 || days > 31) {
            throw new IllegalArgumentException("days must be between 1 and 31");
        }
        LocalDate start = from != null ? from : today();

        Set<LocalDateTime> taken = appointmentRepository
                .findAppointmentsBetween(start.atStartOfDay(), start.plusDays(days).atStartOfDay())
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        LocalDateTime now = now();
        List<DayAvailability> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            result.add(SlotRules.availabilityFor(start.plusDays(i), taken, now, bookingHorizonDays));
        }
        return result;
    }

    // ───────────────────────── Reading ─────────────────────────

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(String email) {
        Patient patient = patientRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return toResponses(appointmentRepository.findByPatientIdOrderByAppointmentTimeDesc(patient.getId()));
    }

    /** Doctor: all appointments, optionally filtered by status and/or date range (both dates inclusive). */
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAllAppointments(
            AppointmentStatus status, LocalDate from, LocalDate to, Pageable pageable) {

        // Start with "match everything", then add one condition per filter that was provided.
        Specification<Appointment> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            LocalDateTime start = from.atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("appointmentTime"), start));
        }
        if (to != null) {
            LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.<LocalDateTime>get("appointmentTime"), endExclusive));
        }

        Page<Appointment> page = appointmentRepository.findAll(spec, pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodayAppointments() {
        LocalDateTime startOfDay = today().atStartOfDay();
        LocalDateTime endOfDay = today().atTime(LocalTime.MAX);
        return toResponses(appointmentRepository.findAppointmentsBetween(startOfDay, endOfDay));
    }

    /** Doctor: one patient's appointment history, newest first. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(Long patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        return toResponses(appointmentRepository.findByPatientIdOrderByAppointmentTimeDesc(patientId));
    }

    // ───────────────────────── Changing status ─────────────────────────

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String requestorEmail, boolean isAdmin) {
        Appointment appointment = loadForUser(id, requestorEmail, isAdmin);

        if (!isActive(appointment)) {
            throw new InvalidStateTransitionException("Only booked or confirmed appointments can be cancelled. Current status: "
                    + appointment.getStatus());
        }

        // Time-based cancellation cutoff for patients
        if (!isAdmin) {
            LocalDateTime cutoff = now().plusHours(cancellationCutoffHours);
            if (appointment.getAppointmentTime().isBefore(cutoff)) {
                throw new InvalidStateTransitionException(
                        "Appointments cannot be cancelled less than " + cancellationCutoffHours
                                + " hours before their scheduled time.");
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setUpdatedAt(now());
        return toResponse(appointmentRepository.save(appointment));
    }

    /** Doctor accepts a booking: SCHEDULED to CONFIRMED. */
    @Transactional
    public AppointmentResponse confirmAppointment(Long id) {
        Appointment appointment = findOrThrow(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Only booked appointments can be confirmed. Current status: "
                    + appointment.getStatus());
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setUpdatedAt(now());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse completeAppointment(Long id) {
        Appointment appointment = findOrThrow(id);
        if (!isActive(appointment)) {
            throw new InvalidStateTransitionException("Only booked or confirmed appointments can be marked complete. Current status: "
                    + appointment.getStatus());
        }
        if (appointment.getAppointmentTime().isAfter(now())) {
            throw new InvalidStateTransitionException("An appointment cannot be completed before its start time.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(now());
        return toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Move an appointment to another slot. A patient's change goes back to SCHEDULED
     * because the doctor confirmed the OLD time, not the new one.
     */
    @Transactional
    public AppointmentResponse rescheduleAppointment(
            Long id, String requestorEmail, boolean isAdmin, LocalDateTime newTime) {

        Appointment appointment = loadForUser(id, requestorEmail, isAdmin);

        if (!isActive(appointment)) {
            throw new InvalidStateTransitionException("Only booked or confirmed appointments can be rescheduled. Current status: "
                    + appointment.getStatus());
        }

        // Time-based rescheduling cutoff for patients
        if (!isAdmin) {
            LocalDateTime cutoff = now().plusHours(cancellationCutoffHours);
            if (appointment.getAppointmentTime().isBefore(cutoff)) {
                throw new InvalidStateTransitionException(
                        "Appointments cannot be rescheduled less than " + cancellationCutoffHours
                                + " hours before their scheduled time.");
            }
        }

        if (newTime.equals(appointment.getAppointmentTime())) {
            throw new IllegalArgumentException("That is already the appointment time. Please choose a different slot.");
        }
        SlotRules.validateBookable(newTime, now(), bookingHorizonDays);
        checkSlotFree(newTime);
        checkOnePerDay(appointment.getPatient().getId(), appointment.getId(), newTime);

        appointment.setAppointmentTime(newTime);
        if (!isAdmin) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }
        appointment.setUpdatedAt(now());
        return toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Housekeeping, every 15 minutes: a booking that the doctor never confirmed and whose DAY has
     * already ended is cancelled, which frees nothing useful but stops dead requests piling up.
     * Same-day bookings are never auto-cancelled, so a patient who turns up for an unconfirmed
     * 10:00 slot is not cancelled at 10:30. Confirmed appointments are left for the doctor to
     * complete or cancel, because the system cannot tell "attended" from "no-show".
     */
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cancelStaleUnconfirmedAppointments() {
        LocalDateTime startOfToday = today().atStartOfDay();
        appointmentRepository.findByStatusAndAppointmentTimeBefore(AppointmentStatus.SCHEDULED, startOfToday)
                .forEach(a -> {
                    a.setStatus(AppointmentStatus.CANCELLED);
                    a.setUpdatedAt(now());
                });
    }

    // ───────────────────────── Helpers ─────────────────────────

    private Appointment findOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    /** Loads the appointment and makes sure this user may touch it: the doctor, or the patient who owns it. */
    private Appointment loadForUser(Long id, String requestorEmail, boolean isAdmin) {
        Appointment appointment = findOrThrow(id);
        String ownerEmail = appointment.getPatient().getUser().getEmail();
        if (!isAdmin && !ownerEmail.equals(requestorEmail)) {
            throw new AccessDeniedException("You can only change your own appointments.");
        }
        return appointment;
    }

    private boolean isActive(Appointment a) {
        return a.getStatus() == AppointmentStatus.SCHEDULED || a.getStatus() == AppointmentStatus.CONFIRMED;
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return new ArrayList<>();
        }
        // One query for the whole list, instead of one query per appointment.
        List<Long> ids = appointments.stream().map(Appointment::getId).collect(Collectors.toList());
        Set<Long> withPrescription = new HashSet<>(prescriptionRepository.findAppointmentIdsWithPrescription(ids));

        return appointments.stream()
                .map(a -> toResponse(a, withPrescription.contains(a.getId())))
                .collect(Collectors.toList());
    }

    private AppointmentResponse toResponse(Appointment a) {
        return toResponse(a, prescriptionRepository.existsByAppointmentId(a.getId()));
    }

    private AppointmentResponse toResponse(Appointment a, boolean hasPrescription) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .appointmentTime(a.getAppointmentTime())
                .status(a.getStatus())
                .reason(a.getReason())
                .patientName(a.getPatient().getName())
                .patientId(a.getPatient().getId())
                .bookedAt(a.getBookedAt())
                .hasPrescription(hasPrescription)
                .patientCanChange(isActive(a)
                        && !a.getAppointmentTime().isBefore(now().plusHours(cancellationCutoffHours)))
                .changeCutoffHours(cancellationCutoffHours)
                .startsInSeconds(java.time.Duration.between(now(), a.getAppointmentTime()).getSeconds())
                .build();
    }
}
