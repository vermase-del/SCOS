package com.scos.modules.facility.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewBookingRequest(
        @NotNull(message = "Approval decision is required")
        Boolean approved,

        String rejectionReason
) {}
