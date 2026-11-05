package com.clinic.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Appointment counts per day / week / month, oldest first. The last bucket is the current period. */
public record TrendResponse(String period, List<Bucket> buckets) {

    /** start and end are both inclusive. total = booked + confirmed + completed + cancelled. */
    public record Bucket(LocalDate start, LocalDate end, int total,
                         int booked, int confirmed, int completed, int cancelled) {}
}
