package com.clinic.service;

import com.clinic.dto.response.DayAvailability;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SlotRulesTest {

    // Wednesday 2026-10-28 10:15
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 10, 15);

    @Test
    void acceptsFutureOpenSlot() {
        assertDoesNotThrow(() -> SlotRules.validateBookable(LocalDateTime.of(2026, 10, 28, 11, 0), NOW, 60));
    }

    @Test
    void rejectsPastSlot() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotRules.validateBookable(LocalDateTime.of(2026, 10, 28, 10, 0), NOW, 60));
    }

    @Test
    void rejectsSlotBeyondHorizon() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotRules.validateBookable(LocalDateTime.of(2027, 3, 3, 10, 0), NOW, 60));
    }

    @Test
    void rejectsSunday() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotRules.validateBookable(LocalDateTime.of(2026, 11, 1, 10, 0), NOW, 60));
    }

    @Test
    void rejectsOffGridMinutes() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotRules.validateBookable(LocalDateTime.of(2026, 10, 29, 10, 15), NOW, 60));
    }

    @Test
    void availabilityMarksDatesBeyondHorizonAsUnavailable() {
        LocalDate withinHorizon = NOW.toLocalDate().plusDays(5);
        LocalDate beyondHorizon = NOW.toLocalDate().plusDays(10);
        int horizonDays = 7;

        DayAvailability dayWithin = SlotRules.availabilityFor(withinHorizon, Collections.emptySet(), NOW, horizonDays);
        assertTrue(dayWithin.availableCount() > 0, "Slots within horizon should be available");

        DayAvailability dayBeyond = SlotRules.availabilityFor(beyondHorizon, Collections.emptySet(), NOW, horizonDays);
        assertEquals(0, dayBeyond.availableCount(), "Slots beyond horizon must have 0 free slots");
        assertFalse(dayBeyond.slots().isEmpty());
        assertTrue(dayBeyond.slots().stream().noneMatch(s -> s.available()),
                "All slots beyond booking horizon must be marked unavailable");
    }
}
