package com.scos.modules.facility.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "Facility ID is required")
        UUID facilityId,

        UUID unitId,

        @NotNull(message = "Booking date is required")
        @FutureOrPresent(message = "Booking date must be today or in the future")
        LocalDate bookingDate,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        Instant endTime,

        @Min(value = 1, message = "Attendees count must be at least 1")
        Integer attendeesCount,

        String purpose
) {}
