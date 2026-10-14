package com.scos.modules.maintenance.dto;
import com.scos.modules.maintenance.entity.CostType; import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record AddCostLineRequest(@NotNull CostType costType,@NotBlank @Size(max=160) String itemDescription,@NotNull @DecimalMin(value="0.01") BigDecimal quantity,@NotNull @DecimalMin(value="0.00") BigDecimal unitPrice,@Size(min=3,max=3) String currencyCode) {}
