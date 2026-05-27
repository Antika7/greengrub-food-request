package com.greengrub.food_request.grpc;

import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Unit;
import com.greengrub.food_request.service.FoodRequestService;
import com.greengrub.proto.foods.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodGrpcServiceTest {

    @Mock
    private FoodRequestService foodRequestService;

    @InjectMocks
    private FoodGrpcService grpcService;

    private FoodRequestDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = FoodRequestDTO.builder()
                .id("food-001")
                .foodName("Wheat")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("3.0")).unit(Unit.KG).build())
                .requestedBy("user-001")
                .requestedDate(LocalDateTime.of(2024, 2, 1, 8, 0))
                .usedByDate(LocalDateTime.of(2024, 4, 1, 0, 0))
                .status(FoodStatus.PENDING)
                .images(List.of())
                .imageIds(List.of())
                .createdAt(LocalDateTime.of(2024, 2, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2024, 2, 1, 8, 0))
                .build();
    }

    // ── createFood ────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void createFood_success_callsOnNextAndCompleted() {
        when(foodRequestService.create(any())).thenReturn(sampleDto);
        StreamObserver<CreateFoodResponse> observer = mock(StreamObserver.class);

        CreateFoodRequest request = CreateFoodRequest.newBuilder()
                .setFoodName("Wheat")
                .setRequestedBy("user-001")
                .setQuantity(Quantity.newBuilder().setAmount(3.0).setUnit(com.greengrub.proto.foods.Unit.KG).build())
                .build();

        grpcService.createFood(request, observer);

        ArgumentCaptor<CreateFoodResponse> captor = ArgumentCaptor.forClass(CreateFoodResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFood().getId()).isEqualTo("food-001");
        assertThat(captor.getValue().getMessage()).isEqualTo("Food request created");
    }

    // ── getFood ───────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getFood_success_returnsFood() {
        when(foodRequestService.getById("food-001")).thenReturn(sampleDto);
        StreamObserver<GetFoodResponse> observer = mock(StreamObserver.class);

        grpcService.getFood(GetFoodRequest.newBuilder().setFoodId("food-001").build(), observer);

        ArgumentCaptor<GetFoodResponse> captor = ArgumentCaptor.forClass(GetFoodResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFood().getFoodName()).isEqualTo("Wheat");
    }

    // ── getFoodsByUser ────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getFoodsByUser_defaultPageSize_usesDefaultOf20() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1);
        when(foodRequestService.getByUser(eq("user-001"), eq(0), eq(20))).thenReturn(page);
        StreamObserver<GetFoodsByUserResponse> observer = mock(StreamObserver.class);

        grpcService.getFoodsByUser(
                GetFoodsByUserRequest.newBuilder().setUserId("user-001").setPage(0).setSize(0).build(),
                observer);

        ArgumentCaptor<GetFoodsByUserResponse> captor = ArgumentCaptor.forClass(GetFoodsByUserResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getTotalCount()).isEqualTo(1);
        assertThat(captor.getValue().getFoodsCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFoodsByUser_negativePage_clampsToZero() {
        var page = new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 10), 1);
        when(foodRequestService.getByUser(eq("user-001"), eq(0), eq(10))).thenReturn(page);
        StreamObserver<GetFoodsByUserResponse> observer = mock(StreamObserver.class);

        grpcService.getFoodsByUser(
                GetFoodsByUserRequest.newBuilder().setUserId("user-001").setPage(-1).setSize(10).build(),
                observer);

        verify(foodRequestService).getByUser("user-001", 0, 10);
    }

    // ── getFoodsByIds ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getFoodsByIds_returnsFoodsAndCount() {
        when(foodRequestService.getByIds(List.of("food-001"))).thenReturn(List.of(sampleDto));
        StreamObserver<GetFoodsByIdsResponse> observer = mock(StreamObserver.class);

        grpcService.getFoodsByIds(
                GetFoodsByIdsRequest.newBuilder().addFoodIds("food-001").build(),
                observer);

        ArgumentCaptor<GetFoodsByIdsResponse> captor = ArgumentCaptor.forClass(GetFoodsByIdsResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getTotalCount()).isEqualTo(1);
        assertThat(captor.getValue().getFoodsCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFoodsByIds_empty_returnsTotalCountZero() {
        when(foodRequestService.getByIds(List.of())).thenReturn(List.of());
        StreamObserver<GetFoodsByIdsResponse> observer = mock(StreamObserver.class);

        grpcService.getFoodsByIds(GetFoodsByIdsRequest.newBuilder().build(), observer);

        ArgumentCaptor<GetFoodsByIdsResponse> captor = ArgumentCaptor.forClass(GetFoodsByIdsResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getTotalCount()).isEqualTo(0);
    }

    // ── updateFoodStatus ──────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void updateFoodStatus_success_returnsUpdatedFood() {
        sampleDto.setStatus(FoodStatus.APPROVED);
        when(foodRequestService.updateStatus(eq("food-001"), any())).thenReturn(sampleDto);
        StreamObserver<UpdateFoodStatusResponse> observer = mock(StreamObserver.class);

        grpcService.updateFoodStatus(
                UpdateFoodStatusRequest.newBuilder().setFoodId("food-001").setStatus("APPROVED").build(),
                observer);

        ArgumentCaptor<UpdateFoodStatusResponse> captor = ArgumentCaptor.forClass(UpdateFoodStatusResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFood().getStatus()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getMessage()).isEqualTo("Status updated");
    }
}
