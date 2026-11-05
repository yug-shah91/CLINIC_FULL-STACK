package com.clinic.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;         // e.g. "Paracetamol"

    @Column(length = 50)
    private String dosage;       // e.g. "500mg"

    @Column(length = 100)
    private String frequency;    // e.g. "Twice a day after meals"

    @Column(name = "duration_days")
    private Integer durationDays; // e.g. 5

    @Column(length = 300)
    private String instructions; // any special instructions

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    private Prescription prescription;
}
