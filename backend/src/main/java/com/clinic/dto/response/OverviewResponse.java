package com.clinic.dto.response;

/** The four numbers on the doctor's dashboard cards. */
public record OverviewResponse(
        long totalPatients,
        long todayAppointments,      // today's appointments that are not cancelled
        long pendingAppointments,    // booked, upcoming, and not yet confirmed by the doctor
        long completedThisMonth) {}
