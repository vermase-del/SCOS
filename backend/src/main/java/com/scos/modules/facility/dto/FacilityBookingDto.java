package com.scos.modules.facility.dto;

import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.entity.FacilityCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FacilityBookingDto(
        UUID id,
        UUID communityId,
        String bookingReference,
        UUID facilityId,
        String facilityCode,
        String facilityName,
        FacilityCategory facilityCategory,
        UUID residentUserId,
        UUID unitId,
        LocalDate bookingDate,
        Instant startTime,
        Instant endTime,
        Integer attendeesCount,
        String purpose,
        BookingStatus status,
        BigDecimal totalFee,
        BigDecimal depositAmount,
        String currencyCode,
        UUID approvedByUserId,
        Instant approvedAt,
        String rejectionReason,
        UUID cancelledByUserId,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt
) {}
