package com.scos.modules.maintenance.dto;
import jakarta.validation.constraints.*;
public record SubmitFeedbackRequest(@Min(1) @Max(5) int rating,@NotNull Boolean resolutionConfirmed,@Size(max=4000) String feedbackComments) {}
