package com.greengrub.food_request.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_requests")
public class FoodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "status", nullable = false)
    private String status;

    // Default constructor is required by JPA
    public FoodRequest() {
    }

    // Parameterized constructor (optional, but helpful)
    public FoodRequest(String foodName, Integer quantity, String requestedBy, LocalDateTime requestDate, String status) {
        this.foodName = foodName;
        this.quantity = quantity;
        this.requestedBy = requestedBy;
        this.requestDate = requestDate;
        this.status = status;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}