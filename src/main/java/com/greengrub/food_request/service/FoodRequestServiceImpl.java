package com.greengrub.food_request.service;

import com.greengrub.food_request.model.FoodRequest;
import com.greengrub.food_request.repository.FoodRequestRepository;
import com.greengrub.food_request.service.impl.FoodRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodRequestServiceImpl implements FoodRequestService {

    @Autowired
    private FoodRequestRepository foodRequestRepository;

    @Override
    public FoodRequest createFoodRequest(FoodRequest request) {
        return foodRequestRepository.save(request);
    }

    @Override
    public List<FoodRequest> getAllFoodRequests() {
        return foodRequestRepository.findAll();
    }

    @Override
    public FoodRequest getFoodRequestById(Long id) {
        // Returns the entity if found, otherwise returns null
        return foodRequestRepository.findById(id).orElse(null);
    }

    @Override
    public FoodRequest updateFoodRequest(Long id, FoodRequest request) {
        FoodRequest existingRequest = foodRequestRepository.findById(id).orElse(null);

        if (existingRequest != null) {
            // Update the fields
            existingRequest.setFoodName(request.getFoodName());
            existingRequest.setQuantity(request.getQuantity());
            existingRequest.setRequestedBy(request.getRequestedBy());
            existingRequest.setRequestDate(request.getRequestDate());
            existingRequest.setStatus(request.getStatus());

            // Save and return the updated entity
            return foodRequestRepository.save(existingRequest);
        }

        return null; // In a production app, throwing a custom ResourceNotFoundException is common here
    }

    @Override
    public void deleteFoodRequest(Long id) {
        foodRequestRepository.deleteById(id);
    }
}