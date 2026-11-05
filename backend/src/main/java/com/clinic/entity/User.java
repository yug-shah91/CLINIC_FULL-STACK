package com.clinic.entity;

import com.clinic.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nullable = false → DB enforces NOT NULL at database level
    @Column(nullable = false, length = 100)
    private String name;

    // unique = true → DB enforces UNIQUE constraint. Index above speeds up lookups by email.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // NEVER store plain text. BCrypt hash is stored here. 60 chars minimum for BCrypt.
    @Column(nullable = false)
    private String password;

    // EnumType.STRING stores 'ADMIN' or 'PATIENT' as text in DB.
    // EnumType.ORDINAL stores 0 or 1 — NEVER use ORDINAL, it breaks if you reorder the enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // @CreationTimestamp: Hibernate automatically sets this to current time on INSERT.
    // updatable = false: this column can never be changed after creation.
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void normalizeEmail() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}
