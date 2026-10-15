package com.scos.modules.facility.dto;

import com.scos.modules.facility.entity.FacilityCategory;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record FacilityDto(
        UUID id,
        UUID communityId,
        String facilityCode,
        String name,
        String description,
        FacilityCategory category,
        UUID buildingId,
        String locationDetails,
        Integer capacity,
        BigDecimal hourlyFee,
        BigDecimal securityDeposit,
        String currencyCode,
        Boolean requiresApproval,
        LocalTime operatingHoursStart,
        LocalTime operatingHoursEnd,
        Integer minBookingHours,
        Integer maxBookingHours,
        Integer cancellationCutoffHours,
        String rulesAndRegulations,
        Boolean isActive
) {}
