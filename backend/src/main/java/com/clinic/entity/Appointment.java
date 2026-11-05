package com.clinic.entity;

import com.clinic.entity.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_time", columnList = "appointment_time"),
        @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LocalDateTime stores both date AND time. Example: 2025-04-10T10:30:00
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    // Stored as text: 'SCHEDULED', 'COMPLETED', 'CANCELLED'
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(length = 300)
    private String reason;

    // Why patient is the owning side: FK 'patient_id' is in appointments table.
    // nullable = false: every appointment MUST have a patient.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @ToString.Exclude
    private Patient patient;

    // When was this appointment created (booked)
    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    // When did the doctor mark it as completed or cancelled
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
