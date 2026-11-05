package com.clinic.service;

import com.clinic.dto.request.AppointmentRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.DayAvailability;
import com.clinic.entity.Appointment;
import com.clinic.entity.Patient;
import com.clinic.entity.User;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.exception.InvalidStateTransitionException;
import com.clinic.exception.SlotConflictException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;

    private Clock clock;
    private AppointmentService appointmentService;

    // Fixed time: Wednesday 2026-10-28 10:00:00 Asia/Kolkata
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 28, 10, 0);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE);
        appointmentService = new AppointmentService(
                appointmentRepository, patientRepository, prescriptionRepository, clock);
        ReflectionTestUtils.setField(appointmentService, "bookingHorizonDays", 7);
        ReflectionTestUtils.setField(appointmentService, "cancellationCutoffHours", 2);
    }

    @Test
    void getAvailabilityEnforcesBookingHorizon() {
        LocalDate today = FIXED_NOW.toLocalDate();
        when(appointmentRepository.findAppointmentsBetween(any(), any())).thenReturn(Collections.emptyList());

        // Ask for 14 days availability when horizon is 7 days
        List<DayAvailability> availability = appointmentService.getAvailability(today, 14);

        assertEquals(14, availability.size());
        // Day 0 to 7 (within horizon) have slots available
        assertTrue(availability.get(1).availableCount() > 0, "Slots within 7 days horizon should be available");

        // Day 8 to 13 (beyond horizon) must have 0 free slots
        for (int i = 8; i < 14; i++) {
            assertEquals(0, availability.get(i).availableCount(),
                    "Day " + i + " is beyond 7-day horizon and must have 0 free slots");
            assertTrue(availability.get(i).slots().stream().noneMatch(s -> s.available()),
                    "All slots for day " + i + " must have available=false");
        }
    }

    @Test
    void bookAppointmentRejectsSlotBeyondBookingHorizon() {
        User user = User.builder().email("patient@example.com").build();
        Patient patient = Patient.builder().id(1L).user(user).build();
        when(patientRepository.findByUserEmail("patient@example.com")).thenReturn(Optional.of(patient));

        AppointmentRequest request = new AppointmentRequest();
        // 10 days in future (horizon is 7)
        request.setAppointmentTime(FIXED_NOW.plusDays(10).withHour(11).withMinute(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> appointmentService.bookAppointment("patient@example.com", request));

        assertTrue(ex.getMessage().contains("can only be booked up to 7 days ahead"));
    }

    @Test
    void bookAppointmentRejectsAlreadyBookedSlot() {
        User user = User.builder().email("patient@example.com").build();
        Patient patient = Patient.builder().id(1L).user(user).build();
        when(patientRepository.findByUserEmail("patient@example.com")).thenReturn(Optional.of(patient));

        LocalDateTime requestedTime = FIXED_NOW.plusDays(1).withHour(11).withMinute(0);
        AppointmentRequest request = new AppointmentRequest();
        request.setAppointmentTime(requestedTime);

        when(appointmentRepository.existsByAppointmentTimeAndStatusNot(requestedTime, AppointmentStatus.CANCELLED))
                .thenReturn(true);

        SlotConflictException ex = assertThrows(SlotConflictException.class,
                () -> appointmentService.bookAppointment("patient@example.com", request));

        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    void cancelAppointmentEnforcesCutoffForPatient() {
        User user = User.builder().email("patient@example.com").build();
        Patient patient = Patient.builder().id(1L).user(user).build();
        // Appointment is in 1 hour (cutoff is 2 hours)
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .appointmentTime(FIXED_NOW.plusHours(1))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> appointmentService.cancelAppointment(10L, "patient@example.com", false));

        assertTrue(ex.getMessage().contains("cannot be cancelled less than 2 hours"));
    }

    @Test
    void cancelAppointmentAllowedForPatientOutsideCutoff() {
        User user = User.builder().email("patient@example.com").build();
        Patient patient = Patient.builder().id(1L).user(user).build();
        // Appointment is tomorrow (more than 2 hours away)
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .appointmentTime(FIXED_NOW.plusDays(1))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(10L, "patient@example.com", false);
        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
    }

    @Test
    void cancelAppointmentAllowedForAdminEvenWithinCutoff() {
        User user = User.builder().email("patient@example.com").build();
        Patient patient = Patient.builder().id(1L).user(user).build();
        // Appointment is in 30 minutes
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .appointmentTime(FIXED_NOW.plusMinutes(30))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(10L, "admin@clinic.com", true);
        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
    }

    @Test
    void completeAppointmentBeforeStartTimeThrowsException() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .status(AppointmentStatus.CONFIRMED)
                .appointmentTime(FIXED_NOW.plusHours(2)) // in future
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThrows(InvalidStateTransitionException.class,
                () -> appointmentService.completeAppointment(10L));
    }
}
