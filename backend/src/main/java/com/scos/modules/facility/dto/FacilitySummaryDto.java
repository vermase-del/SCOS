package com.scos.modules.facility.dto;

public record FacilitySummaryDto(
        long totalFacilities,
        long activeFacilities,
        long totalBookings,
        long bookingsToday,
        long pendingApprovals
) {}
