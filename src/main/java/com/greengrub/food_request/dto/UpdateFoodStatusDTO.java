package com.greengrub.food_request.dto;

import com.greengrub.food_request.entity.FoodStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFoodStatusDTO {

    @NotNull(message = "Status is required")
    private FoodStatus status;
}
