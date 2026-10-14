package com.scos.modules.maintenance.dto;
import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size; import java.util.UUID;
public record AssignTechnicianRequest(@NotNull UUID technicianId,@Size(max=255) String notes) {}
