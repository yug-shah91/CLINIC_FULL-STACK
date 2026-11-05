package com.clinic.repository;

import com.clinic.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Find patient by their linked User ID
    // Spring generates: SELECT * FROM patients WHERE user_id = ?
    Optional<Patient> findByUserId(Long userId);

    // Find patient by their linked User email (joins users table)
    // JPQL uses class names and field names, NOT table/column names
    @Query("SELECT p FROM Patient p WHERE p.user.email = :email")
    Optional<Patient> findByUserEmail(@Param("email") String email);

    // Search patients by name — LIKE '%query%' for partial match
    // Page<Patient> with Pageable for pagination — prevents loading 10,000 patients at once
    // Search by name OR phone number. Sorting comes from the Pageable (e.g. sort=name,asc).
    @Query("SELECT p FROM Patient p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR p.phone LIKE CONCAT('%', :query, '%')")
    Page<Patient> searchByNameOrPhone(@Param("query") String query, Pageable pageable);

    // Check if phone number already registered
    boolean existsByPhone(String phone);
}
