package com.greengrub.food_request.controller;

import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Unit;
import com.greengrub.food_request.service.FoodRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodRequestControllerTest {

    @Mock
    private FoodRequestService foodRequestService;

    @InjectMocks
    private FoodRequestController controller;

    private FoodRequestDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = FoodRequestDTO.builder()
                .id("food-001")
                .foodName("Barley")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("2.0")).unit(Unit.KG).build())
                .requestedBy("user-001")
                .status(FoodStatus.PENDING)
                .createdAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .images(List.of())
                .imageIds(List.of())
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_returns201WithBody() {
        when(foodRequestService.create(any(CreateFoodRequestDTO.class))).thenReturn(sampleDto);

        CreateFoodRequestDTO dto = CreateFoodRequestDTO.builder()
                .foodName("Barley")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("2.0")).unit(Unit.KG).build())
                .requestedBy("user-001")
                .build();

        ResponseEntity<FoodRequestDTO> response = controller.create(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo("food-001");
        assertThat(response.getBody().getFoodName()).isEqualTo("Barley");
    }

    @Test
    void create_delegatesToService() {
        when(foodRequestService.create(any())).thenReturn(sampleDto);
        CreateFoodRequestDTO dto = CreateFoodRequestDTO.builder()
                .foodName("Corn")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("1.0")).unit(Unit.SERVINGS).build())
                .requestedBy("user-002")
                .build();

        controller.create(dto);
        verify(foodRequestService).create(dto);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_returns200() {
        when(foodRequestService.getById("food-001")).thenReturn(sampleDto);

        ResponseEntity<FoodRequestDTO> response = controller.getById("food-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo("food-001");
    }

    @Test
    void getById_delegatesToService() {
        when(foodRequestService.getById("food-XYZ")).thenReturn(sampleDto);
        controller.getById("food-XYZ");
        verify(foodRequestService).getById("food-XYZ");
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returns200() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1);
        when(foodRequestService.getAll(0, 20)).thenReturn(page);

        ResponseEntity<?> response = controller.getAll(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAll_delegatesToService() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1);
        when(foodRequestService.getAll(0, 20)).thenReturn(page);

        controller.getAll(0, 20);

        verify(foodRequestService).getAll(0, 20);
    }

    @Test
    void getAll_returnsPageContent() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1);
        when(foodRequestService.getAll(0, 20)).thenReturn(page);

        ResponseEntity<org.springframework.data.domain.Page<FoodRequestDTO>> response = controller.getAll(0, 20);

        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo("food-001");
    }

    // ── getByUser ─────────────────────────────────────────────────────────────

    @Test
    void getByUser_returnsPage() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1);
        when(foodRequestService.getByUser("user-001", 0, 20)).thenReturn(page);

        ResponseEntity<?> response = controller.getByUser("user-001", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getByUser_defaultPagination() {
        var page = new PageImpl<FoodRequestDTO>(List.of(), PageRequest.of(0, 20), 0);
        when(foodRequestService.getByUser("user-001", 0, 20)).thenReturn(page);

        controller.getByUser("user-001", 0, 20);

        verify(foodRequestService).getByUser("user-001", 0, 20);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_returns200() {
        sampleDto.setStatus(FoodStatus.APPROVED);
        when(foodRequestService.updateStatus(eq("food-001"), any())).thenReturn(sampleDto);

        UpdateFoodStatusDTO dto = UpdateFoodStatusDTO.builder().status(FoodStatus.APPROVED).build();
        ResponseEntity<FoodRequestDTO> response = controller.updateStatus("food-001", dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(FoodStatus.APPROVED);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returns204() {
        doNothing().when(foodRequestService).delete("food-001");

        ResponseEntity<Void> response = controller.delete("food-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(foodRequestService).delete("food-001");
    }
}
