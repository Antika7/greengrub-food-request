package com.greengrub.food_request.service;

import com.greengrub.food_request.exception.ResourceNotFoundException;
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
        return foodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodRequest not found with id: " + id));
    }

    @Override
    public FoodRequest updateFoodRequest(Long id, FoodRequest request) {
        FoodRequest existingRequest = foodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodRequest not found with id: " + id));

        existingRequest.setFoodName(request.getFoodName());
        existingRequest.setQuantity(request.getQuantity());
        existingRequest.setRequestedBy(request.getRequestedBy());
        existingRequest.setRequestDate(request.getRequestDate());
        existingRequest.setStatus(request.getStatus());

        return foodRequestRepository.save(existingRequest);
    }

    @Override
    public void deleteFoodRequest(Long id) {
        if (!foodRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("FoodRequest not found with id: " + id);
        }
        foodRequestRepository.deleteById(id);
    }
}
