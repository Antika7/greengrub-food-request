package com.greengrub.food_request.repository;

import com.greengrub.food_request.entity.FoodRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRequestRepository extends JpaRepository<FoodRequest, String> {

    Page<FoodRequest> findByRequestedBy(String userId, Pageable pageable);

    // Single SQL: SELECT … WHERE id IN (?, ?, …). EntityGraph forces the
    // imageIds @ElementCollection to load in the same query — without it
    // each row would lazy-load a separate SELECT (N+1).
    @EntityGraph(attributePaths = "imageIds")
    List<FoodRequest> findAllByIdIn(List<String> ids);
}
