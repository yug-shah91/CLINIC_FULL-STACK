package com.clinic.service;

import com.clinic.dto.response.TrendResponse;
import com.clinic.entity.enums.AppointmentStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Groups appointments into day / week / month buckets for the Reports page. No Spring, easy to test. */
public final class ReportBuckets {

    public enum Period {
        DAILY(14),    // last 14 days, ending today
        WEEKLY(8),    // last 8 weeks (Monday to Sunday), ending with the current week
        MONTHLY(6);   // last 6 months, ending with the current month

        public final int buckets;

        Period(int buckets) {
            this.buckets = buckets;
        }

        public static Period parse(String text) {
            try {
                return valueOf(text.trim().toUpperCase());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("period must be DAILY, WEEKLY or MONTHLY");
            }
        }
    }

    /** The only two things we need from an appointment. */
    public record Point(LocalDateTime time, AppointmentStatus status) {}

    private ReportBuckets() {}

    /** {start, end} (both inclusive) for every bucket, oldest first. */
    public static List<LocalDate[]> ranges(Period period, LocalDate today) {
        List<LocalDate[]> ranges = new ArrayList<>();
        for (int i = period.buckets - 1; i >= 0; i--) {
            switch (period) {
                case DAILY -> {
                    LocalDate day = today.minusDays(i);
                    ranges.add(new LocalDate[]{day, day});
                }
                case WEEKLY -> {
                    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(i);
                    ranges.add(new LocalDate[]{monday, monday.plusDays(6)});
                }
                case MONTHLY -> {
                    LocalDate first = today.withDayOfMonth(1).minusMonths(i);
                    ranges.add(new LocalDate[]{first, first.with(TemporalAdjusters.lastDayOfMonth())});
                }
            }
        }
        return ranges;
    }

    public static TrendResponse build(Period period, LocalDate today, List<Point> points) {
        List<LocalDate[]> ranges = ranges(period, today);
        int[][] counts = new int[ranges.size()][4]; // booked, confirmed, completed, cancelled

        for (Point p : points) {
            LocalDate day = p.time().toLocalDate();
            for (int i = 0; i < ranges.size(); i++) {
                LocalDate[] r = ranges.get(i);
                if (!day.isBefore(r[0]) && !day.isAfter(r[1])) {
                    switch (p.status()) {
                        case SCHEDULED -> counts[i][0]++;
                        case CONFIRMED -> counts[i][1]++;
                        case COMPLETED -> counts[i][2]++;
                        case CANCELLED -> counts[i][3]++;
                    }
                    break;
                }
            }
        }

        List<TrendResponse.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            int[] c = counts[i];
            buckets.add(new TrendResponse.Bucket(
                    ranges.get(i)[0], ranges.get(i)[1], c[0] + c[1] + c[2] + c[3], c[0], c[1], c[2], c[3]));
        }
        return new TrendResponse(period.name(), buckets);
    }
}
