package com.clinic.service;

import com.clinic.entity.MedicalRecordAudit;
import com.clinic.repository.MedicalRecordAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private MedicalRecordAuditRepository auditRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);
        auditService = new AuditService(auditRepository, fixed);
    }

    private MedicalRecordAudit saved() {
        ArgumentCaptor<MedicalRecordAudit> captor = ArgumentCaptor.forClass(MedicalRecordAudit.class);
        verify(auditRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void storesLongValuesCompleteWithoutTruncation() {
        String longOld = "old-".repeat(3000);   // 12,000 characters
        String longNew = "new-".repeat(3000);
        String longDetails = "detail ".repeat(500);

        auditService.recordChange("Prescription", 7L, "CREATE", "doctor@clinic.com",
                longOld, longNew, longDetails);

        MedicalRecordAudit record = saved();
        assertEquals(longOld, record.getOldValue());
        assertEquals(longNew, record.getNewValue());
        assertEquals(longDetails, record.getDetails());
    }

    @Test
    void recordsWhoWhatAndWhenFromTheClinicClock() {
        auditService.recordChange("Patient", 3L, "UPDATE", "doctor@clinic.com", "a", "b", "note");

        MedicalRecordAudit record = saved();
        assertEquals("Patient", record.getEntityName());
        assertEquals(3L, record.getEntityId());
        assertEquals("UPDATE", record.getAction());
        assertEquals("doctor@clinic.com", record.getPerformedBy());
        assertEquals(java.time.LocalDateTime.of(2026, 10, 28, 10, 0), record.getPerformedAt());
    }

    @Test
    void fallsBackToSystemWhenNoActorIsGiven() {
        auditService.recordChange("Patient", 3L, "UPDATE", null, null, null, null);

        assertEquals("SYSTEM", saved().getPerformedBy());
    }
}
