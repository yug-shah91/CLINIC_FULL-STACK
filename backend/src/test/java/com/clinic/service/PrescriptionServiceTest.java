package com.clinic.service;

import com.clinic.dto.request.PrescriptionRequest;
import com.clinic.dto.response.PrescriptionResponse;
import com.clinic.entity.Appointment;
import com.clinic.entity.MedicalRecordAudit;
import com.clinic.entity.Patient;
import com.clinic.entity.Prescription;
import com.clinic.entity.User;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.InvalidStateTransitionException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.MedicalRecordAuditRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MedicalRecordAuditRepository auditRepository;

    private AuditService auditService;
    private PrescriptionService prescriptionService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditRepository, Clock.systemUTC());
        prescriptionService = new PrescriptionService(
                prescriptionRepository, appointmentRepository, patientRepository, auditService);
    }

    @Test
    void getMyPrescriptionThrowsAccessDeniedExceptionWhenOwnershipDoesNotMatch() {
        User owner = User.builder().email("patient1@example.com").build();
        Patient patient = Patient.builder().id(1L).user(owner).name("Patient One").build();
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .appointmentTime(LocalDateTime.now())
                .build();
        Prescription prescription = Prescription.builder()
                .id(100L)
                .appointment(appointment)
                .diagnosis("Cold")
                .build();

        when(prescriptionRepository.findByAppointmentId(10L)).thenReturn(Optional.of(prescription));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> prescriptionService.getMyPrescription(10L, "attacker@example.com"));

        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void getMyPrescriptionSucceedsForOwner() {
        User owner = User.builder().email("patient1@example.com").build();
        Patient patient = Patient.builder().id(1L).user(owner).name("Patient One").build();
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .appointmentTime(LocalDateTime.now())
                .build();
        Prescription prescription = Prescription.builder()
                .id(100L)
                .appointment(appointment)
                .diagnosis("Flu")
                .build();

        when(prescriptionRepository.findByAppointmentId(10L)).thenReturn(Optional.of(prescription));

        PrescriptionResponse response = prescriptionService.getMyPrescription(10L, "patient1@example.com");
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Flu", response.getDiagnosis());
    }

    @Test
    void writePrescriptionFailsIfAppointmentNotCompleted() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        PrescriptionRequest request = new PrescriptionRequest();
        request.setAppointmentId(10L);
        request.setDiagnosis("Fever");

        assertThrows(InvalidStateTransitionException.class,
                () -> prescriptionService.writePrescription(request, "doctor@clinic.com"));
    }

    @Test
    void writePrescriptionFailsIfAlreadyExists() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(prescriptionRepository.existsByAppointmentId(10L)).thenReturn(true);

        PrescriptionRequest request = new PrescriptionRequest();
        request.setAppointmentId(10L);
        request.setDiagnosis("Fever");

        assertThrows(DuplicateResourceException.class,
                () -> prescriptionService.writePrescription(request, "doctor@clinic.com"));
    }

    @Test
    void writePrescriptionSucceedsAndRecordsAudit() {
        User user = User.builder().email("alice@test.com").name("Alice").build();
        Patient patient = Patient.builder().id(1L).user(user).name("Alice").build();
        Appointment appointment = Appointment.builder()
                .id(10L)
                .patient(patient)
                .status(AppointmentStatus.COMPLETED)
                .appointmentTime(LocalDateTime.now())
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(prescriptionRepository.existsByAppointmentId(10L)).thenReturn(false);

        Prescription saved = Prescription.builder()
                .id(50L)
                .appointment(appointment)
                .diagnosis("Strep Throat")
                .build();
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(saved);

        PrescriptionRequest request = new PrescriptionRequest();
        request.setAppointmentId(10L);
        request.setDiagnosis("Strep Throat");

        PrescriptionResponse response = prescriptionService.writePrescription(request, "doctor@clinic.com");
        assertNotNull(response);
        assertEquals(50L, response.getId());

        ArgumentCaptor<MedicalRecordAudit> auditCaptor = ArgumentCaptor.forClass(MedicalRecordAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("PRESCRIPTION", auditCaptor.getValue().getEntityName());
        assertEquals(50L, auditCaptor.getValue().getEntityId());
        assertEquals("doctor@clinic.com", auditCaptor.getValue().getPerformedBy());
    }
}
