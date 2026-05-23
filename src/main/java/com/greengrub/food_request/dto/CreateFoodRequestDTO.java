package com.greengrub.food_request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFoodRequestDTO {

    @NotBlank(message = "Food name cannot be blank")
    private String foodName;

    @NotNull(message = "Quantity is required")
    @Valid
    private QuantityDTO quantity;

    @NotBlank(message = "requestedBy cannot be blank")
    private String requestedBy;

    private LocalDateTime usedByDate;

    // Raw bytes of attached images. Uploaded to image-service after the food
    // row is persisted (so creator_id = food UUID for later hydration).
    @Builder.Default
    private List<byte[]> imageData = new ArrayList<>();

    // Optional metadata for the upload batch — image-service expects a single
    // file_name / content_type per UploadImagesRequest.
    private String fileName;
    private String contentType;
}
