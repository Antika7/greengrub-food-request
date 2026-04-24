package com.greengrub.food_request.controller;

import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.model.FoodRequest;
import com.greengrub.food_request.service.impl.FoodRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Food Requests", description = "CRUD operations for food requests")
@RestController
@RequestMapping("/api/foodRequests")
public class FoodRequestController {

    private final FoodRequestService foodRequestService;

    public FoodRequestController(FoodRequestService foodRequestService) {
        this.foodRequestService = foodRequestService;
    }

    @Operation(summary = "Get all food requests", description = "Returns a list of all food requests.")
    @ApiResponse(responseCode = "200", description = "List of food requests returned successfully.")
    @GetMapping
    public ResponseEntity<List<FoodRequest>> getAllFoodRequests() {
        return ResponseEntity.ok(foodRequestService.getAllFoodRequests());
    }

    @Operation(summary = "Get a food request by ID", description = "Returns a single food request by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food request found."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FoodRequest> getFoodRequestById(
            @Parameter(description = "ID of the food request to retrieve") @PathVariable Long id) {
        return ResponseEntity.ok(foodRequestService.getFoodRequestById(id));
    }

    @Operation(summary = "Create a new food request", description = "Creates a new food request and returns it.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Food request created successfully."),
        @ApiResponse(responseCode = "400", description = "Invalid request body.")
    })
    @PostMapping
    public ResponseEntity<FoodRequest> createFoodRequest(@Valid @RequestBody FoodRequestDTO dto) {
        FoodRequest request = toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(foodRequestService.createFoodRequest(request));
    }

    @Operation(summary = "Update a food request", description = "Updates an existing food request by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food request updated successfully."),
        @ApiResponse(responseCode = "400", description = "Invalid request body."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @PutMapping("/{id}")
    public ResponseEntity<FoodRequest> updateFoodRequest(
            @Parameter(description = "ID of the food request to update") @PathVariable Long id,
            @Valid @RequestBody FoodRequestDTO dto) {
        FoodRequest request = toEntity(dto);
        return ResponseEntity.ok(foodRequestService.updateFoodRequest(id, request));
    }

    @Operation(summary = "Delete a food request", description = "Deletes a food request by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Food request deleted successfully."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFoodRequest(
            @Parameter(description = "ID of the food request to delete") @PathVariable Long id) {
        foodRequestService.deleteFoodRequest(id);
        return ResponseEntity.noContent().build();
    }

    private FoodRequest toEntity(FoodRequestDTO dto) {
        FoodRequest request = new FoodRequest();
        request.setFoodName(dto.getFoodName());
        request.setQuantity(dto.getQuantity());
        request.setRequestedBy(dto.getRequestedBy());
        request.setStatus(dto.getStatus());
        request.setRequestDate(dto.getRequestDate() != null ? dto.getRequestDate() : LocalDateTime.now());
        return request;
    }
}
