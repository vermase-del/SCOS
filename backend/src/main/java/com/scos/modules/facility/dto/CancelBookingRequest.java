package com.scos.modules.facility.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelBookingRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {}
