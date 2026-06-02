package com.greengrub.food_request.service.impl;

import com.greengrub.food_request.client.ImageServiceClient;
import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.ImageDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import com.greengrub.food_request.entity.FoodRequest;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Quantity;
import com.greengrub.food_request.exception.FoodNotFoundException;
import com.greengrub.food_request.exception.ImageUploadFailedException;
import com.greengrub.food_request.repository.FoodRequestRepository;
import com.greengrub.food_request.service.FoodRequestService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRequestServiceImpl implements FoodRequestService {

    private final FoodRequestRepository repository;
    private final ImageServiceClient imageServiceClient;

    @Override
    @Transactional
    public FoodRequestDTO create(CreateFoodRequestDTO dto) {
        // 1. Persist the food row first so we have a UUID to use as creator_id.
        FoodRequest entity = FoodRequest.builder()
                .foodName(dto.getFoodName())
                .quantity(toEntityQuantity(dto.getQuantity()))
                .requestedBy(dto.getRequestedBy())
                .usedByDate(dto.getUsedByDate())
                .status(FoodStatus.PENDING)
                .imageIds(new ArrayList<>())
                .build();
        FoodRequest saved = saveFood(entity);

        // 2. Upload bytes (if any) — synchronous wait on the TimeLimiter future.
        //    Failure here is a 502 since the food row is meaningless without its images
        //    if the user attached them.
        if (dto.getImageData() != null && !dto.getImageData().isEmpty()) {
            try {
                List<String> imageIds = imageServiceClient.uploadImages(
                        saved.getId(), dto.getImageData(), dto.getFileName(), dto.getContentType()
                ).get();
                saved.setImageIds(imageIds);
                saved = saveFood(saved);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ImageUploadFailedException("Image upload interrupted", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new ImageUploadFailedException(
                        "Image upload failed: " + cause.getMessage(), cause);
            }
        }

        // 3. Hydrate the response with image URLs from image-service.
        return toDto(saved, hydrateImages(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public FoodRequestDTO getById(String id) {
        FoodRequest entity = findById(id);
        return toDto(entity, hydrateImages(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodRequestDTO> getByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FoodRequest> rows = findByUser(userId, pageable);
        return rows.map(food -> toDto(food, hydrateImages(food.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodRequestDTO> getByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // Single SQL "WHERE id IN (?, ?, …)" with imageIds eager-loaded via @EntityGraph.
        List<FoodRequest> rows = findAllByIdIn(ids);

        // Hydrate images in parallel — one gRPC call per food, but launched concurrently
        // so total latency is bounded by the slowest call rather than their sum.
        List<CompletableFuture<List<ImageDTO>>> futures = rows.stream()
                .map(food -> imageServiceClient.getImagesByCreator(food.getId())
                        .exceptionally(ex -> {
                            log.warn("Image hydration failed for foodId {} — falling back to empty: {}",
                                    food.getId(), ex.getMessage());
                            return List.of();
                        }))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return rows.stream().map(food -> toDto(food, List.<ImageDTO>of())).toList();
        } catch (ExecutionException ignored) {
            // Per-food exceptions are already swallowed above; this is unreachable.
        }

        List<FoodRequestDTO> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            List<ImageDTO> images = futures.get(i).getNow(List.of());
            out.add(toDto(rows.get(i), images));
        }
        return out;
    }

    @Override
    @Transactional
    public FoodRequestDTO updateStatus(String id, UpdateFoodStatusDTO dto) {
        FoodRequest entity = findById(id);
        entity.setStatus(dto.getStatus());
        FoodRequest saved = saveFood(entity);
        return toDto(saved, hydrateImages(id));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new FoodNotFoundException(id);
        }
        deleteFood(id);
    }

    // --- Resilience-wrapped DB helpers ---

    @CircuitBreaker(name = "dbBreaker")
    private FoodRequest saveFood(FoodRequest entity) {
        // No retry on save — JPA save is not idempotent under retry.
        return repository.save(entity);
    }

    @Retry(name = "dbRetry")
    @CircuitBreaker(name = "dbBreaker")
    private FoodRequest findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new FoodNotFoundException(id));
    }

    @Retry(name = "dbRetry")
    @CircuitBreaker(name = "dbBreaker")
    private Page<FoodRequest> findByUser(String userId, Pageable pageable) {
        return repository.findByRequestedBy(userId, pageable);
    }

    @Retry(name = "dbRetry")
    @CircuitBreaker(name = "dbBreaker")
    private List<FoodRequest> findAllByIdIn(List<String> ids) {
        return repository.findAllByIdIn(ids);
    }

    @CircuitBreaker(name = "dbBreaker")
    private void deleteFood(String id) {
        repository.deleteById(id);
    }

    /**
     * Convenience wrapper for single-food hydration. Falls back to an empty list
     * on circuit-open or timeout — degraded UX (no thumbnail), not a 5xx.
     */
    private List<ImageDTO> hydrateImages(String creatorId) {
        try {
            return imageServiceClient.getImagesByCreator(creatorId).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("Image hydration failed for creatorId {} — falling back to empty: {}",
                    creatorId, e.getMessage());
            return List.of();
        }
    }

    // --- Mapping helpers ---

    private static Quantity toEntityQuantity(QuantityDTO dto) {
        return new Quantity(dto.getAmount(), dto.getUnit());
    }

    private static QuantityDTO toDtoQuantity(Quantity entity) {
        return QuantityDTO.builder()
                .amount(entity.getAmount())
                .unit(entity.getUnit())
                .build();
    }

    private static FoodRequestDTO toDto(FoodRequest entity, List<ImageDTO> images) {
        return FoodRequestDTO.builder()
                .id(entity.getId())
                .foodName(entity.getFoodName())
                .quantity(toDtoQuantity(entity.getQuantity()))
                .requestedBy(entity.getRequestedBy())
                .requestedDate(entity.getRequestedDate())
                .usedByDate(entity.getUsedByDate())
                .status(entity.getStatus())
                .imageIds(new ArrayList<>(entity.getImageIds()))
                .images(images != null ? images : List.of())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
