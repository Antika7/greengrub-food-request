package com.greengrub.food_request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class ContributeDTO {

    @NotBlank(message = "contributorName is required")
    private String contributorName;

    @Positive(message = "quantityOffered must be greater than 0")
    private double quantityOffered;

    private String additionalDetails;
}
