package com.greengrub.food_request.controller;

import com.greengrub.food_request.model.FoodRequest;
import com.greengrub.food_request.repository.FoodRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Food Requests", description = "CRUD operations for food requests")
@RestController
@RequestMapping("/api/foodRequests")
public class FoodRequestController {

    private final FoodRequestRepository repository;

    public FoodRequestController(FoodRequestRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Get all food requests", description = "Returns a list of all food requests.")
    @ApiResponse(responseCode = "200", description = "List of food requests returned successfully.")
    @GetMapping
    public List<FoodRequest> getAllFoodRequests() {
        return repository.findAll();
    }

    @Operation(summary = "Get a food request by ID", description = "Returns a single food request by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food request found."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @GetMapping("/{id}")
    public FoodRequest getFoodRequestById(@Parameter(description = "ID of the food request to retrieve") @PathVariable Long id) {
        // Returns null if not found (in a real app, returning a 404 Exception is better)
        return repository.findById(id).orElse(null);
    }

    @Operation(summary = "Create a new food request", description = "Creates a new food request and returns it.")
    @ApiResponse(responseCode = "201", description = "Food request created successfully.")
    @PostMapping
    public FoodRequest createFoodRequest(@RequestBody FoodRequest foodRequest) {
        return repository.save(foodRequest);
    }

    @Operation(summary = "Update a food request", description = "Updates an existing food request by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food request updated successfully."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @PutMapping("/{id}")
    public FoodRequest updateFoodRequest(
            @Parameter(description = "ID of the food request to update") @PathVariable Long id,
            @RequestBody FoodRequest foodRequestDetails) {
        return repository.findById(id).map(existingRequest -> {
            existingRequest.setFoodName(foodRequestDetails.getFoodName());
            existingRequest.setQuantity(foodRequestDetails.getQuantity());
            existingRequest.setRequestedBy(foodRequestDetails.getRequestedBy());
            existingRequest.setRequestDate(foodRequestDetails.getRequestDate());
            existingRequest.setStatus(foodRequestDetails.getStatus());
            return repository.save(existingRequest);
        }).orElse(null); // Returns null if the ID doesn't exist
    }

    @Operation(summary = "Delete a food request", description = "Deletes a food request by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Food request deleted successfully."),
        @ApiResponse(responseCode = "404", description = "Food request not found.")
    })
    @DeleteMapping("/{id}")
    public void deleteFoodRequest(@Parameter(description = "ID of the food request to delete") @PathVariable Long id) {
        repository.deleteById(id);
    }
}
