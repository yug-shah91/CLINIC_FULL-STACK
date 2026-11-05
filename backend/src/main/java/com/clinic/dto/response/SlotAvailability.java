package com.clinic.dto.response;

/** One 30-minute slot. time is "HH:mm", e.g. "09:30". */
public record SlotAvailability(String time, boolean available) {}
