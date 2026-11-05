package com.clinic.repository;

import com.clinic.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Find prescription for a specific appointment
    Optional<Prescription> findByAppointmentId(Long appointmentId);

    // Check if a prescription already exists for this appointment (prevent duplicates)
    boolean existsByAppointmentId(Long appointmentId);

    // Which of these appointments already have a prescription? One query for a whole page of appointments.
    @Query("SELECT pr.appointment.id FROM Prescription pr WHERE pr.appointment.id IN :ids")
    List<Long> findAppointmentIdsWithPrescription(@Param("ids") Collection<Long> ids);

    // Get all prescriptions for a patient (their medical history)
    // Joins through Appointment to reach Patient
    @Query("SELECT pr FROM Prescription pr " +
           "WHERE pr.appointment.patient.id = :patientId " +
           "ORDER BY pr.createdAt DESC")
    List<Prescription> findAllByPatientId(@Param("patientId") Long patientId);
}
