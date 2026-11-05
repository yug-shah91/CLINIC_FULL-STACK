package com.clinic.service;

import com.clinic.dto.response.DayAvailability;
import com.clinic.dto.response.SlotAvailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The clinic's booking rules in ONE place, used both when booking/rescheduling (validate)
 * and when showing free slots (availabilityFor). No Spring here, so it is easy to test.
 */
public final class SlotRules {

    public static final LocalTime OPEN = LocalTime.of(9, 0);
    public static final LocalTime CLOSE = LocalTime.of(17, 0);
    public static final int SLOT_MINUTES = 30;

    private SlotRules() {}

    public static boolean isOpenDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** 09:00, 09:30 ... 16:30 */
    public static List<LocalTime> slotTimes() {
        List<LocalTime> times = new ArrayList<>();
        for (LocalTime t = OPEN; t.isBefore(CLOSE); t = t.plusMinutes(SLOT_MINUTES)) {
            times.add(t);
        }
        return times;
    }

    /**
     * Throws IllegalArgumentException (a RuntimeException), which GlobalExceptionHandler
     * turns into HTTP 400 with the message below.
     */
    public static void validate(LocalDateTime dateTime) {
        if (!isOpenDay(dateTime.toLocalDate())) {
            throw new IllegalArgumentException("Clinic is closed on Sundays.");
        }
        LocalTime time = dateTime.toLocalTime();
        LocalTime lastSlot = CLOSE.minusMinutes(SLOT_MINUTES); // 4:30 PM
        if (time.isBefore(OPEN) || time.isAfter(lastSlot)) {
            throw new IllegalArgumentException(
                    "Appointments only available between 9:00 AM and 4:30 PM (Mon-Sat).");
        }
        if (dateTime.getMinute() != 0 && dateTime.getMinute() != 30) {
            throw new IllegalArgumentException(
                    "Appointments must start at :00 or :30. E.g., 10:00 or 10:30.");
        }
        if (dateTime.getSecond() != 0 || dateTime.getNano() != 0) {
            throw new IllegalArgumentException("Seconds must be 0 for appointment times.");
        }
    }

    /**
     * validate() plus the rules that depend on "now": the slot must still be in the future and
     * not further ahead than the booking horizon.
     */
    public static void validateBookable(LocalDateTime dateTime, LocalDateTime now, int horizonDays) {
        validate(dateTime);
        if (!dateTime.isAfter(now)) {
            throw new IllegalArgumentException("Appointment must be scheduled in the future.");
        }
        if (dateTime.toLocalDate().isAfter(now.toLocalDate().plusDays(horizonDays))) {
            throw new IllegalArgumentException(
                    "Appointments can only be booked up to " + horizonDays + " days ahead.");
        }
    }

    /**
     * @param taken       start times of appointments that are not cancelled
     * @param now         slots that have already started are not available
     * @param horizonDays maximum days ahead that can be booked (slots beyond horizon are unavailable)
     */
    public static DayAvailability availabilityFor(LocalDate date, Set<LocalDateTime> taken, LocalDateTime now, int horizonDays) {
        if (!isOpenDay(date)) {
            return new DayAvailability(date, true, 0, List.of());
        }
        boolean beyondHorizon = date.isAfter(now.toLocalDate().plusDays(horizonDays));
        List<SlotAvailability> slots = new ArrayList<>();
        int free = 0;
        for (LocalTime t : slotTimes()) {
            LocalDateTime start = date.atTime(t);
            boolean available = !beyondHorizon && start.isAfter(now) && !taken.contains(start);
            if (available) free++;
            slots.add(new SlotAvailability(t.toString(), available));
        }
        return new DayAvailability(date, false, free, slots);
    }

    /** Overload for backwards compatibility without horizon limit. */
    public static DayAvailability availabilityFor(LocalDate date, Set<LocalDateTime> taken, LocalDateTime now) {
        return availabilityFor(date, taken, now, Integer.MAX_VALUE);
    }
}
