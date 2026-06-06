package com.greengrub.food_request.service;

import com.greengrub.food_request.dto.ContributeDTO;
import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FoodRequestService {

    FoodRequestDTO create(CreateFoodRequestDTO dto);

    FoodRequestDTO getById(String id);

    FoodRequestDTO uploadImage(String id, MultipartFile file);

    FoodRequestDTO contribute(String id, ContributeDTO dto);

    Page<FoodRequestDTO> getAll(int page, int size);

    Page<FoodRequestDTO> getByUser(String userId, int page, int size);

    List<FoodRequestDTO> getByIds(List<String> ids);

    FoodRequestDTO updateStatus(String id, UpdateFoodStatusDTO dto);

    void delete(String id);
}
