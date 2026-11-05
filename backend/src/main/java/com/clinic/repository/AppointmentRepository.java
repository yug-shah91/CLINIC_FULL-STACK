package com.clinic.repository;

import com.clinic.entity.Appointment;
import com.clinic.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// JpaSpecificationExecutor lets the service build the admin filters (status, date range) dynamically.
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    // Get all appointments for a specific patient, newest first
    List<Appointment> findByPatientIdOrderByAppointmentTimeDesc(Long patientId);

    // Is this exact slot already used by an appointment that is not cancelled?
    // Slots are always :00 or :30 (see SlotRules), so an exact match is enough.
    boolean existsByAppointmentTimeAndStatusNot(LocalDateTime appointmentTime, AppointmentStatus status);

    // Get all appointments between two dates (doctor's daily schedule, availability, reports)
    @Query("SELECT a FROM Appointment a WHERE a.appointmentTime BETWEEN :start AND :end ORDER BY a.appointmentTime ASC")
    List<Appointment> findAppointmentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Booked appointments that are still upcoming (dashboard "pending" card)
    long countByStatusAndAppointmentTimeAfter(AppointmentStatus status, LocalDateTime time);

    long countByStatus(AppointmentStatus status);

    // One active appointment per patient per day (excludes the appointment being rescheduled)
    boolean existsByPatientIdAndIdNotAndAppointmentTimeBetweenAndStatusIn(
            Long patientId, Long excludeId, LocalDateTime start, LocalDateTime end,
            java.util.Collection<AppointmentStatus> statuses);

    // Used by the stale-booking cleanup
    List<Appointment> findByStatusAndAppointmentTimeBefore(AppointmentStatus status, LocalDateTime time);
}
