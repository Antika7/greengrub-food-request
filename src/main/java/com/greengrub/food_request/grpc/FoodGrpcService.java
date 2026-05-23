package com.greengrub.food_request.grpc;

import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.mapper.FoodMapper;
import com.greengrub.food_request.service.FoodRequestService;
import com.greengrub.proto.foods.CreateFoodRequest;
import com.greengrub.proto.foods.CreateFoodResponse;
import com.greengrub.proto.foods.FoodServiceGrpc;
import com.greengrub.proto.foods.GetFoodRequest;
import com.greengrub.proto.foods.GetFoodResponse;
import com.greengrub.proto.foods.GetFoodsByIdsRequest;
import com.greengrub.proto.foods.GetFoodsByIdsResponse;
import com.greengrub.proto.foods.GetFoodsByUserRequest;
import com.greengrub.proto.foods.GetFoodsByUserResponse;
import com.greengrub.proto.foods.UpdateFoodStatusRequest;
import com.greengrub.proto.foods.UpdateFoodStatusResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * gRPC façade over {@link FoodRequestService}. Exception mapping is handled by
 * {@code GrpcExceptionInterceptor} (registered globally via @GrpcGlobalServerInterceptor).
 *
 * {@code GetFoodsByIds} backs donation-service's "fetch foods by id list" lookup
 * — single round trip, single SQL "WHERE id IN (?, ?, …)".
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FoodGrpcService extends FoodServiceGrpc.FoodServiceImplBase {

    private final FoodRequestService foodRequestService;

    @Override
    public void createFood(CreateFoodRequest request, StreamObserver<CreateFoodResponse> responseObserver) {
        FoodRequestDTO created = foodRequestService.create(FoodMapper.toCreateDto(request));
        CreateFoodResponse response = CreateFoodResponse.newBuilder()
                .setFood(FoodMapper.toProtoFood(created))
                .setMessage("Food request created")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFood(GetFoodRequest request, StreamObserver<GetFoodResponse> responseObserver) {
        FoodRequestDTO food = foodRequestService.getById(request.getFoodId());
        GetFoodResponse response = GetFoodResponse.newBuilder()
                .setFood(FoodMapper.toProtoFood(food))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFoodsByUser(GetFoodsByUserRequest request, StreamObserver<GetFoodsByUserResponse> responseObserver) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() > 0 ? request.getSize() : 20;
        Page<FoodRequestDTO> pageOut = foodRequestService.getByUser(request.getUserId(), page, size);

        GetFoodsByUserResponse.Builder builder = GetFoodsByUserResponse.newBuilder()
                .setTotalCount((int) pageOut.getTotalElements());
        for (FoodRequestDTO food : pageOut.getContent()) {
            builder.addFoods(FoodMapper.toProtoFood(food));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getFoodsByIds(GetFoodsByIdsRequest request, StreamObserver<GetFoodsByIdsResponse> responseObserver) {
        List<FoodRequestDTO> foods = foodRequestService.getByIds(request.getFoodIdsList());

        GetFoodsByIdsResponse.Builder builder = GetFoodsByIdsResponse.newBuilder()
                .setTotalCount(foods.size());
        for (FoodRequestDTO food : foods) {
            builder.addFoods(FoodMapper.toProtoFood(food));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateFoodStatus(UpdateFoodStatusRequest request, StreamObserver<UpdateFoodStatusResponse> responseObserver) {
        FoodRequestDTO updated = foodRequestService.updateStatus(
                request.getFoodId(), FoodMapper.toUpdateStatusDto(request));

        UpdateFoodStatusResponse response = UpdateFoodStatusResponse.newBuilder()
                .setFood(FoodMapper.toProtoFood(updated))
                .setMessage("Status updated")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
