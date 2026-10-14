package com.scos.modules.maintenance.dto;
import com.scos.modules.maintenance.entity.CostType; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record WorkOrderCostLineDto(UUID id,CostType costType,String itemDescription,BigDecimal quantity,BigDecimal unitPrice,BigDecimal totalAmount,String currencyCode,UUID recordedByUserId,Instant createdAt) {}
