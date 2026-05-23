package com.greengrub.food_request.dto;

import com.greengrub.food_request.entity.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuantityDTO {

    @NotNull(message = "Quantity amount is required")
    @DecimalMin(value = "0.01", message = "Quantity amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Quantity unit is required")
    private Unit unit;
}
