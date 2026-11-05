package com.clinic.service;

import com.clinic.dto.response.OverviewResponse;
import com.clinic.dto.response.TrendResponse;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Numbers for the doctor's dashboard and the Reports page.
 * A clinic books a few dozen appointments a day, so loading the rows and counting in Java is fine.
 * The counting rules live in ReportBuckets, where they are unit-tested.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final java.time.Clock clock;

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        LocalDate today = LocalDate.now(clock);

        long todayCount = appointmentRepository
                .findAppointmentsBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX))
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .count();

        long pending = appointmentRepository.countByStatusAndAppointmentTimeAfter(
                AppointmentStatus.SCHEDULED, LocalDateTime.now(clock));

        LocalDate monthStart = today.withDayOfMonth(1);
        long completedThisMonth = appointmentRepository
                .findAppointmentsBetween(monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay())
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        return new OverviewResponse(patientRepository.count(), todayCount, pending, completedThisMonth);
    }

    @Transactional(readOnly = true)
    public TrendResponse appointmentTrend(String periodText) {
        ReportBuckets.Period period = ReportBuckets.Period.parse(periodText);
        LocalDate today = LocalDate.now(clock);

        List<LocalDate[]> ranges = ReportBuckets.ranges(period, today);
        LocalDateTime from = ranges.get(0)[0].atStartOfDay();
        LocalDateTime to = ranges.get(ranges.size() - 1)[1].plusDays(1).atStartOfDay();

        List<ReportBuckets.Point> points = appointmentRepository.findAppointmentsBetween(from, to).stream()
                .map(a -> new ReportBuckets.Point(a.getAppointmentTime(), a.getStatus()))
                .collect(Collectors.toList());

        return ReportBuckets.build(period, today, points);
    }
}
