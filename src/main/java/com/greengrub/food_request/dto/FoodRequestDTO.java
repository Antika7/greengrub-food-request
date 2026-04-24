package com.greengrub.food_request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FoodRequestDTO {

    @NotBlank(message = "Food name cannot be blank")
    private String foodName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Requested by cannot be blank")
    private String requestedBy;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private LocalDateTime requestDate;

    // Default constructor
    public FoodRequestDTO() {
    }

    // Parameterized constructor
    public FoodRequestDTO(String foodName, Integer quantity, String requestedBy, String status) {
        this.foodName = foodName;
        this.quantity = quantity;
        this.requestedBy = requestedBy;
        this.status = status;
    }

    // --- Getters and Setters ---

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }
}