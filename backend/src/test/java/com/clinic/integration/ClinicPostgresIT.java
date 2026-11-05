package com.clinic.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real PostgreSQL + Spring Security filter-chain checks.
 * Run with: ./mvnw verify (or mvnw.cmd verify); Docker is required.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class ClinicPostgresIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> "integration-test-secret-that-is-at-least-32-bytes");
        registry.add("app.admin.email", () -> "");
        registry.add("app.admin.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void protectedPatientEndpointReturnsUniformJson401WithoutToken() throws Exception {
        mvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void flywayCreatesTheExpectedAppointmentIntegrityIndexes() {
        String activeSlot = jdbc.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'ux_appointments_active_slot'
                """, String.class);
        String patientDay = jdbc.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'ux_appointments_patient_active_day'
                """, String.class);

        assertNotNull(activeSlot);
        assertTrue(activeSlot.toLowerCase().contains("unique"));
        assertTrue(activeSlot.toLowerCase().contains("appointment_time"));
        assertTrue(activeSlot.toLowerCase().contains("status"));

        assertNotNull(patientDay);
        assertTrue(patientDay.toLowerCase().contains("unique"));
        assertTrue(patientDay.toLowerCase().contains("patient_id"));
        assertTrue(patientDay.toLowerCase().contains("appointment_time"));
    }

    @Test
    void postgresRejectsTwoActiveAppointmentsForOnePatientOnTheSameDay() {
        long userId = insertUser("patient-" + UUID.randomUUID() + "@clinic.test");
        long patientId = jdbc.queryForObject("""
                INSERT INTO patients(name, user_id) VALUES ('Integration Patient', ?)
                RETURNING id
                """, Long.class, userId);

        Timestamp first = Timestamp.valueOf(LocalDateTime.of(2035, 5, 10, 10, 0));
        Timestamp second = Timestamp.valueOf(LocalDateTime.of(2035, 5, 10, 11, 0));

        jdbc.update("""
                INSERT INTO appointments(patient_id, appointment_time, status)
                VALUES (?, ?, 'SCHEDULED')
                """, patientId, first);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO appointments(patient_id, appointment_time, status)
                VALUES (?, ?, 'CONFIRMED')
                """, patientId, second));
    }

    @Test
    void concurrentRequestsForTheSameSlotAllowExactlyOneInsert() throws Exception {
        long patientOne = insertPatient("race-one-" + UUID.randomUUID() + "@clinic.test");
        long patientTwo = insertPatient("race-two-" + UUID.randomUUID() + "@clinic.test");
        Timestamp slot = Timestamp.valueOf(LocalDateTime.of(2035, 7, 15, 14, 0));

        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> first = () -> insertAtBarrier(patientOne, slot, ready, start);
            java.util.concurrent.Callable<Boolean> second = () -> insertAtBarrier(patientTwo, slot, ready, start);
            var a = pool.submit(first);
            var b = pool.submit(second);
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();

            int successful = (a.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
                    + (b.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successful, "the PostgreSQL unique index must allow exactly one winner");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private boolean insertAtBarrier(long patientId, Timestamp slot,
                                    java.util.concurrent.CountDownLatch ready,
                                    java.util.concurrent.CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            jdbc.update("""
                    INSERT INTO appointments(patient_id, appointment_time, status)
                    VALUES (?, ?, 'SCHEDULED')
                    """, patientId, slot);
            return true;
        } catch (DataIntegrityViolationException expectedConflict) {
            return false;
        }
    }

    @Test
    void postgresRejectsTwoActivePatientsAtTheSameAppointmentTime() {
        long patientOne = insertPatient("one-" + UUID.randomUUID() + "@clinic.test");
        long patientTwo = insertPatient("two-" + UUID.randomUUID() + "@clinic.test");
        Timestamp slot = Timestamp.valueOf(LocalDateTime.of(2035, 6, 12, 9, 30));

        jdbc.update("""
                INSERT INTO appointments(patient_id, appointment_time, status)
                VALUES (?, ?, 'SCHEDULED')
                """, patientOne, slot);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO appointments(patient_id, appointment_time, status)
                VALUES (?, ?, 'SCHEDULED')
                """, patientTwo, slot));
    }

    private long insertPatient(String email) {
        long userId = insertUser(email);
        return jdbc.queryForObject("""
                INSERT INTO patients(name, user_id) VALUES ('Integration Patient', ?)
                RETURNING id
                """, Long.class, userId);
    }

    private long insertUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users(name, email, password, role)
                VALUES ('Integration User', ?, '$2a$10$integration-test-hash-placeholder', 'PATIENT')
                RETURNING id
                """, Long.class, email);
    }
}
