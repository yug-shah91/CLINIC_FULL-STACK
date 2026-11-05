package com.clinic.service;

import com.clinic.dto.request.MedicalNotesRequest;
import com.clinic.dto.request.PatientRequest;
import com.clinic.dto.response.PatientResponse;
import com.clinic.entity.MedicalRecordAudit;
import com.clinic.entity.Patient;
import com.clinic.entity.User;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.repository.MedicalRecordAuditRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MedicalRecordAuditRepository auditRepository;

    private AuditService auditService;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditRepository, Clock.systemUTC());
        patientService = new PatientService(patientRepository, userRepository, auditService);
    }

    @Test
    void createProfileThrowsDuplicateResourceExceptionWhenProfileAlreadyExists() {
        User user = User.builder().id(1L).email("patient@clinic.com").name("Patient").build();
        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(Patient.builder().id(10L).build()));

        PatientRequest request = new PatientRequest();
        request.setName("Patient");

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> patientService.createProfile("patient@clinic.com", request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(patientRepository, never()).save(any());
    }

    @Test
    void createProfileSucceedsForNewUserAndAuditsInitialNotes() {
        User user = User.builder().id(1L).email("patient@clinic.com").name("Patient").build();
        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Patient saved = Patient.builder()
                .id(10L)
                .user(user)
                .name("Patient")
                .allergies("Latex")
                .medicalHistory("Diabetes")
                .build();
        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        PatientRequest request = new PatientRequest();
        request.setName("Patient");
        request.setAllergies("Latex");
        request.setMedicalHistory("Diabetes");

        PatientResponse response = patientService.createProfile("patient@clinic.com", request);
        assertNotNull(response);
        assertEquals(10L, response.getId());

        ArgumentCaptor<MedicalRecordAudit> auditCaptor = ArgumentCaptor.forClass(MedicalRecordAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("PATIENT_NOTES", auditCaptor.getValue().getEntityName());
        assertEquals("CREATE_NOTES", auditCaptor.getValue().getAction());
        assertEquals("patient@clinic.com", auditCaptor.getValue().getPerformedBy());
        assertTrue(auditCaptor.getValue().getNewValue().contains("Latex"));
    }

    @Test
    void updateProfileAuditsMedicalNotesWhenChangedByPatient() {
        User user = User.builder().id(1L).email("patient@clinic.com").name("Patient").build();
        Patient patient = Patient.builder()
                .id(10L)
                .name("Patient")
                .user(user)
                .allergies("Dust")
                .medicalHistory("None")
                .build();

        when(patientRepository.findByUserEmail("patient@clinic.com")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientRequest request = new PatientRequest();
        request.setAllergies("Dust, Pollen"); // changed

        PatientResponse response = patientService.updateProfile("patient@clinic.com", request);
        assertNotNull(response);

        ArgumentCaptor<MedicalRecordAudit> auditCaptor = ArgumentCaptor.forClass(MedicalRecordAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("PATIENT_NOTES", auditCaptor.getValue().getEntityName());
        assertEquals("UPDATE_NOTES", auditCaptor.getValue().getAction());
        assertEquals("patient@clinic.com", auditCaptor.getValue().getPerformedBy());
        assertTrue(auditCaptor.getValue().getOldValue().contains("Dust"));
        assertTrue(auditCaptor.getValue().getNewValue().contains("Pollen"));
    }

    @Test
    void updateProfileDoesNotAuditWhenMedicalNotesAreNotChanged() {
        User user = User.builder().id(1L).email("patient@clinic.com").name("Patient").build();
        Patient patient = Patient.builder()
                .id(10L)
                .name("Patient")
                .user(user)
                .phone("1234567890")
                .allergies("Dust")
                .medicalHistory("None")
                .build();

        when(patientRepository.findByUserEmail("patient@clinic.com")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientRequest request = new PatientRequest();
        request.setPhone("9876543210"); // Only phone changed, allergies/history untouched

        PatientResponse response = patientService.updateProfile("patient@clinic.com", request);
        assertNotNull(response);

        verify(auditRepository, never()).save(any());
    }

    @Test
    void updateMedicalNotesAuditsChanges() {
        User user = User.builder().id(1L).email("patient@clinic.com").name("Patient").build();
        Patient patient = Patient.builder()
                .id(10L)
                .name("Patient")
                .user(user)
                .allergies("Peanuts")
                .medicalHistory("Asthma")
                .build();

        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicalNotesRequest notesRequest = new MedicalNotesRequest("Penicillin, Peanuts", "Asthma, Mild hypertension");
        PatientResponse response = patientService.updateMedicalNotes(10L, notesRequest, "doctor@clinic.com");

        assertNotNull(response);
        assertEquals("Penicillin, Peanuts", response.getAllergies());

        ArgumentCaptor<MedicalRecordAudit> auditCaptor = ArgumentCaptor.forClass(MedicalRecordAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("PATIENT_NOTES", auditCaptor.getValue().getEntityName());
        assertEquals(10L, auditCaptor.getValue().getEntityId());
        assertEquals("doctor@clinic.com", auditCaptor.getValue().getPerformedBy());
        assertTrue(auditCaptor.getValue().getOldValue().contains("Peanuts"));
        assertTrue(auditCaptor.getValue().getNewValue().contains("Penicillin"));
    }
}
