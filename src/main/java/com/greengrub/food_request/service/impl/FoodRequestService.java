package com.greengrub.food_request.service.impl;

import com.greengrub.food_request.model.FoodRequest;

import java.util.List;

public interface FoodRequestService {

    // Creates and returns the saved food request
    FoodRequest createFoodRequest(FoodRequest request);

    // Retrieves a list of all food requests
    List<FoodRequest> getAllFoodRequests();

    // Retrieves a single food request by its ID
    FoodRequest getFoodRequestById(Long id);

    // Updates an existing food request and returns the updated entity
    FoodRequest updateFoodRequest(Long id, FoodRequest request);

    // Deletes the food request by its ID (no return value needed)
    void deleteFoodRequest(Long id);

}
