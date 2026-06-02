package com.greengrub.food_request.client;

import com.google.protobuf.ByteString;
import com.greengrub.food_request.dto.ImageDTO;
import com.greengrub.food_request.exception.ImageHydrationFailedException;
import com.greengrub.food_request.exception.ImageUploadFailedException;
import com.greengrub.proto.image.CreatorType;
import com.greengrub.proto.image.GetImagesByCreatorRequest;
import com.greengrub.proto.image.GetImagesByCreatorResponse;
import com.greengrub.proto.image.Image;
import com.greengrub.proto.image.ImageServiceGrpc;
import com.greengrub.proto.image.UploadImagesRequest;
import com.greengrub.proto.image.UploadImagesResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * gRPC client for image-service. Wraps every external call in
 * Retry + CircuitBreaker, plus a TimeLimiter for the (CompletableFuture)
 * async paths so a slow image-service can't tie up the food-service worker.
 *
 * Retry rationale: both upload and read are idempotent enough to retry —
 * UploadImages persists by content+creator and GetImagesByCreator is a pure read.
 */
@Slf4j
@Component
public class ImageServiceClient {

    @GrpcClient("imageService")
    private ImageServiceGrpc.ImageServiceBlockingStub blockingStub;

    /**
     * Upload a batch of image bytes to image-service. The returned ids are
     * stored on the food row so later hydration can fetch them by creator.
     *
     * Time-limited (10s default) and retried up to 3x with exponential backoff.
     * Returns a CompletableFuture so the @TimeLimiter annotation works.
     */
    @TimeLimiter(name = "imageUploadLimiter")
    @Retry(name = "imageRetry")
    @CircuitBreaker(name = "imageBreaker")
    public CompletableFuture<List<String>> uploadImages(
            String creatorId, List<byte[]> imageData, String fileName, String contentType) {

        return CompletableFuture.supplyAsync(() -> {
            if (imageData == null || imageData.isEmpty()) {
                return List.<String>of();
            }
            try {
                UploadImagesRequest.Builder req = UploadImagesRequest.newBuilder()
                        .setCreatorId(creatorId)
                        .setCreatorType(CreatorType.FOOD_REQUEST)
                        .setFileName(fileName != null ? fileName : "")
                        .setContentType(contentType != null ? contentType : "");
                for (byte[] bytes : imageData) {
                    req.addImageData(ByteString.copyFrom(bytes));
                }

                UploadImagesResponse resp = blockingStub.uploadImages(req.build());
                // image-service returns the new image ids in the message list.
                return new ArrayList<>(resp.getMessageList());
            } catch (StatusRuntimeException e) {
                log.warn("image-service uploadImages failed for creatorId {}: {}",
                        creatorId, e.getStatus());
                throw new ImageUploadFailedException(
                        "Failed to upload images to image-service: " + e.getStatus().getDescription(), e);
            }
        });
    }

    /**
     * Hydrate the images attached to a food row. Returns a CompletableFuture
     * so the @TimeLimiter applies. The service layer falls back to an empty
     * list if this future fails — a missing image is degraded UX, not a 5xx.
     */
    @TimeLimiter(name = "imageReadLimiter")
    @Retry(name = "imageRetry")
    @CircuitBreaker(name = "imageBreaker")
    public CompletableFuture<List<ImageDTO>> getImagesByCreator(String creatorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GetImagesByCreatorRequest req = GetImagesByCreatorRequest.newBuilder()
                        .setCreatorId(creatorId)
                        .build();

                // Server-streaming RPC — drain the iterator and flatten every
                // response's images into a single list.
                Iterator<GetImagesByCreatorResponse> stream = blockingStub.getImagesByCreator(req);
                List<ImageDTO> out = new ArrayList<>();
                while (stream.hasNext()) {
                    GetImagesByCreatorResponse chunk = stream.next();
                    out.addAll(chunk.getImagesList().stream()
                            .map(ImageServiceClient::toDto)
                            .collect(Collectors.toList()));
                }
                return out;
            } catch (StatusRuntimeException e) {
                log.warn("image-service getImagesByCreator failed for creatorId {}: {}",
                        creatorId, e.getStatus());
                throw new ImageHydrationFailedException(
                        "Failed to hydrate images for creatorId: " + creatorId, e);
            }
        });
    }

    private static ImageDTO toDto(Image proto) {
        return ImageDTO.builder()
                .id(proto.getImageId())
                .url((proto.hasImageUrl() && !proto.getImageUrl().trim().isEmpty()) ? proto.getImageUrl() : null)
                .image_data((proto.hasImageData()  && !proto.getImageData().isEmpty())? proto.getImageData().toByteArray() :  null)
                .fileName(proto.getFileName())
                .contentType(proto.hasContentType() ? proto.getContentType() : null)
                .build();
    }
}
