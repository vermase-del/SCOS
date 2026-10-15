package com.scos.modules.facility.dto;

import com.scos.modules.facility.entity.FacilityCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record CreateFacilityRequest(
        @NotBlank(message = "Facility code is required")
        String facilityCode,

        @NotBlank(message = "Facility name is required")
        String name,

        String description,

        @NotNull(message = "Facility category is required")
        FacilityCategory category,

        UUID buildingId,
        String locationDetails,

        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @DecimalMin(value = "0.00", message = "Hourly fee cannot be negative")
        BigDecimal hourlyFee,

        @DecimalMin(value = "0.00", message = "Security deposit cannot be negative")
        BigDecimal securityDeposit,

        String currencyCode,
        Boolean requiresApproval,
        LocalTime operatingHoursStart,
        LocalTime operatingHoursEnd,

        @Min(value = 1, message = "Min booking hours must be at least 1")
        Integer minBookingHours,

        @Min(value = 1, message = "Max booking hours must be at least 1")
        Integer maxBookingHours,

        @Min(value = 0, message = "Cancellation cutoff hours cannot be negative")
        Integer cancellationCutoffHours,

        String rulesAndRegulations
) {}
