package com.clinic.entity;

import com.clinic.entity.enums.BloodGroup;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patients_phone", columnList = "phone")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_patients_user_id", columnNames = {"user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 15)
    private String phone;

    @Column(length = 10)
    private String gender;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", length = 15)
    private BloodGroup bloodGroup;

    // Medical record fields. Nullable on purpose: existing rows in the database have no value yet.
    @Column(length = 1000)
    private String allergies;

    @Column(name = "medical_history", length = 2000)
    private String medicalHistory;

    // OneToOne: each patient links to one user account.
    // @JoinColumn(name = "user_id"): creates a column 'user_id' as FK in patients table.
    // This side OWNS the relationship (holds the FK column).
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // OneToMany: one patient has many appointments.
    // mappedBy = "patient": Appointment entity has a field called 'patient' with the FK.
    // We don't hold the FK here — Appointment table does.
    // FetchType.LAZY (default): appointments are NOT loaded from DB unless we call getAppointments().
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;
}
