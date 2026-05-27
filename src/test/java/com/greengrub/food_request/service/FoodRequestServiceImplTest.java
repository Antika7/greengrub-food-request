package com.greengrub.food_request.service;

import com.greengrub.food_request.client.ImageServiceClient;
import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import com.greengrub.food_request.entity.FoodRequest;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Quantity;
import com.greengrub.food_request.entity.Unit;
import com.greengrub.food_request.exception.FoodNotFoundException;
import com.greengrub.food_request.exception.ImageUploadFailedException;
import com.greengrub.food_request.repository.FoodRequestRepository;
import com.greengrub.food_request.service.impl.FoodRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FoodRequestServiceImplTest {

    @Mock
    private FoodRequestRepository repository;

    @Mock
    private ImageServiceClient imageServiceClient;

    @InjectMocks
    private FoodRequestServiceImpl service;

    private FoodRequest sampleEntity;
    private CreateFoodRequestDTO createDto;

    @BeforeEach
    void setUp() {
        sampleEntity = FoodRequest.builder()
                .id("food-001")
                .foodName("Rice")
                .quantity(new Quantity(new BigDecimal("5.0"), Unit.KG))
                .requestedBy("user-001")
                .requestedDate(LocalDateTime.of(2024, 1, 10, 9, 0))
                .usedByDate(LocalDateTime.of(2024, 3, 1, 0, 0))
                .status(FoodStatus.PENDING)
                .imageIds(new ArrayList<>())
                .createdAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .build();

        createDto = CreateFoodRequestDTO.builder()
                .foodName("Rice")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("5.0")).unit(Unit.KG).build())
                .requestedBy("user-001")
                .usedByDate(LocalDateTime.of(2024, 3, 1, 0, 0))
                .imageData(new ArrayList<>())
                .build();

        when(imageServiceClient.getImagesByCreator(anyString()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_noImages_savesAndReturnsDto() {
        when(repository.save(any(FoodRequest.class))).thenReturn(sampleEntity);

        FoodRequestDTO result = service.create(createDto);

        verify(repository).save(any(FoodRequest.class));
        assertThat(result.getFoodName()).isEqualTo("Rice");
        assertThat(result.getStatus()).isEqualTo(FoodStatus.PENDING);
    }

    @Test
    void create_withImages_uploadsAndSavesImageIds() throws Exception {
        when(repository.save(any(FoodRequest.class))).thenReturn(sampleEntity);
        when(imageServiceClient.uploadImages(anyString(), anyList(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(List.of("img-1")));

        createDto.setImageData(List.of(new byte[]{1, 2, 3}));
        createDto.setFileName("photo.jpg");
        createDto.setContentType("image/jpeg");

        FoodRequestDTO result = service.create(createDto);

        verify(imageServiceClient).uploadImages(eq("food-001"), anyList(), eq("photo.jpg"), eq("image/jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void create_uploadFails_throwsImageUploadFailedException() {
        when(repository.save(any(FoodRequest.class))).thenReturn(sampleEntity);
        CompletableFuture<List<String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("gRPC down"));
        when(imageServiceClient.uploadImages(anyString(), anyList(), any(), any()))
                .thenReturn(failedFuture);

        createDto.setImageData(List.of(new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> service.create(createDto))
                .isInstanceOf(ImageUploadFailedException.class);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsDto() {
        when(repository.findById("food-001")).thenReturn(Optional.of(sampleEntity));

        FoodRequestDTO result = service.getById("food-001");

        assertThat(result.getId()).isEqualTo("food-001");
        assertThat(result.getFoodName()).isEqualTo("Rice");
    }

    @Test
    void getById_notFound_throwsFoodNotFoundException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(FoodNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getById_imageFails_fallsBackToEmptyImages() {
        when(repository.findById("food-001")).thenReturn(Optional.of(sampleEntity));
        CompletableFuture<java.util.List<com.greengrub.food_request.dto.ImageDTO>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("image-service down"));
        when(imageServiceClient.getImagesByCreator("food-001")).thenReturn(failed);

        FoodRequestDTO result = service.getById("food-001");

        assertThat(result.getImages()).isEmpty();
    }

    // ── getByUser ─────────────────────────────────────────────────────────────

    @Test
    void getByUser_returnsPaginatedResults() {
        var page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
        when(repository.findByRequestedBy(eq("user-001"), any())).thenReturn(page);

        var result = service.getByUser("user-001", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getByUser_emptyPage_returnsEmpty() {
        var page = new PageImpl<FoodRequest>(List.of(), PageRequest.of(0, 20), 0);
        when(repository.findByRequestedBy(eq("nobody"), any())).thenReturn(page);

        var result = service.getByUser("nobody", 0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    // ── getByIds ──────────────────────────────────────────────────────────────

    @Test
    void getByIds_nullList_returnsEmpty() {
        assertThat(service.getByIds(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void getByIds_emptyList_returnsEmpty() {
        assertThat(service.getByIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void getByIds_validIds_returnsHydratedDtos() {
        when(repository.findAllByIdIn(List.of("food-001"))).thenReturn(List.of(sampleEntity));

        List<FoodRequestDTO> result = service.getByIds(List.of("food-001"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("food-001");
    }

    @Test
    void getByIds_imageFutureFails_fallsBackToEmptyImages() {
        when(repository.findAllByIdIn(anyList())).thenReturn(List.of(sampleEntity));
        CompletableFuture<java.util.List<com.greengrub.food_request.dto.ImageDTO>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("image down"));
        when(imageServiceClient.getImagesByCreator("food-001")).thenReturn(failed);

        List<FoodRequestDTO> result = service.getByIds(List.of("food-001"));

        assertThat(result.get(0).getImages()).isEmpty();
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_found_updatesAndReturns() {
        when(repository.findById("food-001")).thenReturn(Optional.of(sampleEntity));
        when(repository.save(any(FoodRequest.class))).thenReturn(sampleEntity);

        UpdateFoodStatusDTO dto = UpdateFoodStatusDTO.builder().status(FoodStatus.APPROVED).build();
        FoodRequestDTO result = service.updateStatus("food-001", dto);

        assertThat(result).isNotNull();
        verify(repository).save(argThat(e -> e.getStatus() == FoodStatus.APPROVED));
    }

    @Test
    void updateStatus_notFound_throwsFoodNotFoundException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus("missing",
                UpdateFoodStatusDTO.builder().status(FoodStatus.APPROVED).build()))
                .isInstanceOf(FoodNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_existing_callsDeleteById() {
        when(repository.existsById("food-001")).thenReturn(true);
        doNothing().when(repository).deleteById("food-001");

        service.delete("food-001");

        verify(repository).deleteById("food-001");
    }

    @Test
    void delete_notFound_throwsFoodNotFoundException() {
        when(repository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(FoodNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ── branch coverage for create InterruptedException ───────────────────────

    @Test
    void create_uploadExecutionException_throwsImageUploadFailedException() {
        when(repository.save(any(FoodRequest.class))).thenReturn(sampleEntity);
        CompletableFuture<List<String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("downstream failure"));
        when(imageServiceClient.uploadImages(anyString(), anyList(), any(), any()))
                .thenReturn(failed);

        createDto.setImageData(List.of(new byte[]{1}));

        assertThatThrownBy(() -> service.create(createDto))
                .isInstanceOf(ImageUploadFailedException.class)
                .hasMessageContaining("downstream failure");
    }

    // ── getByIds — interrupted branch ─────────────────────────────────────────

    @Test
    void getByIds_withImages_hydratesEachFood() throws Exception {
        FoodRequest food2 = FoodRequest.builder()
                .id("food-002")
                .foodName("Corn")
                .quantity(new Quantity(new BigDecimal("1.0"), Unit.SERVINGS))
                .requestedBy("user-002")
                .status(FoodStatus.PENDING)
                .imageIds(new ArrayList<>())
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();
        when(repository.findAllByIdIn(List.of("food-001", "food-002")))
                .thenReturn(List.of(sampleEntity, food2));

        List<FoodRequestDTO> result = service.getByIds(List.of("food-001", "food-002"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("food-001");
        assertThat(result.get(1).getId()).isEqualTo("food-002");
    }
}
