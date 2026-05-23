package com.greengrub.food_request.dto;

import com.greengrub.food_request.entity.FoodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response shape for read endpoints. {@code images} is hydrated from image-service
 * via a single {@code GetImagesByCreator(food_uuid)} gRPC call per food. Falls
 * back to an empty list when the image-service circuit breaker is open.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodRequestDTO {

    private String id;
    private String foodName;
    private QuantityDTO quantity;
    private String requestedBy;
    private LocalDateTime requestedDate;
    private LocalDateTime usedByDate;
    private FoodStatus status;

    @Builder.Default
    private List<String> imageIds = new ArrayList<>();

    @Builder.Default
    private List<ImageDTO> images = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
