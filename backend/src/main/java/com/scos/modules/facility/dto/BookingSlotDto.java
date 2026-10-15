package com.scos.modules.facility.dto;

import java.time.Instant;

public record BookingSlotDto(
        Instant startTime,
        Instant endTime,
        boolean isAvailable,
        String bookingReference
) {}
