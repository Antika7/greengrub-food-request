package com.greengrub.food_request.controller;

import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import com.greengrub.food_request.service.FoodRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Food Requests", description = "Food request CRUD + status transitions")
@RestController
@RequestMapping("/api/v1/food-requests")
@RequiredArgsConstructor
public class FoodRequestController {

    private final FoodRequestService foodRequestService;

    @Operation(summary = "Create a new food request")
    @PostMapping
    public ResponseEntity<FoodRequestDTO> create(@Valid @RequestBody CreateFoodRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(foodRequestService.create(dto));
    }

    @Operation(summary = "Get a food request by id (with hydrated images)")
    @GetMapping("/{id}")
    public ResponseEntity<FoodRequestDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(foodRequestService.getById(id));
    }

    @Operation(summary = "Page through food requests posted by a user")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<FoodRequestDTO>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(foodRequestService.getByUser(userId, page, size));
    }

    @Operation(summary = "Update the status of a food request")
    @PatchMapping("/{id}/status")
    public ResponseEntity<FoodRequestDTO> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateFoodStatusDTO dto) {
        return ResponseEntity.ok(foodRequestService.updateStatus(id, dto));
    }

    @Operation(summary = "Delete a food request")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        foodRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
