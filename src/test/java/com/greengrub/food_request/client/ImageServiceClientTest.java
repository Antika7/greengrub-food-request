package com.greengrub.food_request.client;

import com.greengrub.food_request.dto.ImageDTO;
import com.greengrub.food_request.exception.ImageHydrationFailedException;
import com.greengrub.food_request.exception.ImageUploadFailedException;
import com.greengrub.proto.image.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImageServiceClientTest {

    private ImageServiceClient client;
    private ImageServiceGrpc.ImageServiceBlockingStub blockingStub;

    @BeforeEach
    void setUp() {
        client = new ImageServiceClient();
        blockingStub = mock(ImageServiceGrpc.ImageServiceBlockingStub.class);
        ReflectionTestUtils.setField(client, "blockingStub", blockingStub);
    }

    // ── uploadImages ──────────────────────────────────────────────────────────

    @Test
    void uploadImages_emptyList_returnsEmptyWithoutCallingStub() throws Exception {
        List<String> result = client.uploadImages("creator-1", List.of(), null, null).get();
        assertThat(result).isEmpty();
        verifyNoInteractions(blockingStub);
    }

    @Test
    void uploadImages_nullList_returnsEmptyWithoutCallingStub() throws Exception {
        List<String> result = client.uploadImages("creator-1", null, null, null).get();
        assertThat(result).isEmpty();
        verifyNoInteractions(blockingStub);
    }

    @Test
    void uploadImages_success_returnsIds() throws Exception {
        UploadImagesResponse response = UploadImagesResponse.newBuilder()
                .addMessage("img-1")
                .addMessage("img-2")
                .build();
        when(blockingStub.uploadImages(any(UploadImagesRequest.class))).thenReturn(response);

        List<String> result = client.uploadImages(
                "creator-1", List.of(new byte[]{1, 2}, new byte[]{3, 4}),
                "photo.jpg", "image/jpeg").get();

        assertThat(result).containsExactly("img-1", "img-2");
    }

    @Test
    void uploadImages_nullFileName_usesEmpty() throws Exception {
        UploadImagesResponse response = UploadImagesResponse.newBuilder().build();
        when(blockingStub.uploadImages(any(UploadImagesRequest.class))).thenReturn(response);

        List<String> result = client.uploadImages("creator-1", List.of(new byte[]{1}), null, null).get();
        assertThat(result).isEmpty();
    }

    @Test
    void uploadImages_grpcFailure_throwsImageUploadFailedException() {
        when(blockingStub.uploadImages(any(UploadImagesRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        var future = client.uploadImages("creator-1", List.of(new byte[]{1}), "f.jpg", "image/jpeg");

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ImageUploadFailedException.class);
    }

    // ── getImagesByCreator ────────────────────────────────────────────────────

    @Test
    void getImagesByCreator_emptyStream_returnsEmptyList() throws Exception {
        Iterator<GetImagesByCreatorResponse> emptyIter = Collections.emptyIterator();
        when(blockingStub.getImagesByCreator(any(GetImagesByCreatorRequest.class))).thenReturn(emptyIter);

        List<ImageDTO> result = client.getImagesByCreator("food-001").get();
        assertThat(result).isEmpty();
    }

    @Test
    void getImagesByCreator_multipleChunks_flattensAll() throws Exception {
        Image img1 = Image.newBuilder().setImageId("id-1").setFileName("a.jpg").build();
        Image img2 = Image.newBuilder().setImageId("id-2").setFileName("b.jpg").build();
        Image img3 = Image.newBuilder().setImageId("id-3").setFileName("c.jpg").build();

        GetImagesByCreatorResponse chunk1 = GetImagesByCreatorResponse.newBuilder()
                .addImages(img1).addImages(img2).build();
        GetImagesByCreatorResponse chunk2 = GetImagesByCreatorResponse.newBuilder()
                .addImages(img3).build();

        Iterator<GetImagesByCreatorResponse> iter = List.of(chunk1, chunk2).iterator();
        when(blockingStub.getImagesByCreator(any())).thenReturn(iter);

        List<ImageDTO> result = client.getImagesByCreator("food-001").get();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo("id-1");
        assertThat(result.get(2).getId()).isEqualTo("id-3");
    }

    @Test
    void getImagesByCreator_imageWithUrl_mapsUrlField() throws Exception {
        Image img = Image.newBuilder()
                .setImageId("id-1")
                .setFileName("photo.jpg")
                .setImageUrl("https://cdn.example.com/photo.jpg")
                .setContentType("image/jpeg")
                .build();

        GetImagesByCreatorResponse chunk = GetImagesByCreatorResponse.newBuilder().addImages(img).build();
        when(blockingStub.getImagesByCreator(any())).thenReturn(List.of(chunk).iterator());

        List<ImageDTO> result = client.getImagesByCreator("food-001").get();
        assertThat(result.get(0).getUrl()).isEqualTo("https://cdn.example.com/photo.jpg");
        assertThat(result.get(0).getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void getImagesByCreator_grpcFailure_throwsImageHydrationFailedException() {
        when(blockingStub.getImagesByCreator(any(GetImagesByCreatorRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INTERNAL));

        var future = client.getImagesByCreator("food-001");

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ImageHydrationFailedException.class);
    }
}
