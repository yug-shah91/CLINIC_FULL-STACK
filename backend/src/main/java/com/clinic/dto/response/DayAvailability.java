package com.clinic.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the booking screen needs about one day.
 * closed = the clinic does not work that day (Sunday), so slots is empty.
 * availableCount = how many slots are still free (0 means the day is full).
 */
public record DayAvailability(LocalDate date, boolean closed, int availableCount, List<SlotAvailability> slots) {}
