package com.greengrub.food_request.repository;

import com.greengrub.food_request.model.FoodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRequestRepository extends JpaRepository<FoodRequest, Long> {

    // Basic CRUD operations (save, findById, findAll, deleteById, etc.)
    // are automatically inherited from JpaRepository.

}