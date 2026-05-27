package com.greengrub.food_request.mapper;

import com.google.protobuf.ByteString;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.ImageDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Unit;
import com.greengrub.proto.foods.Food;
import com.greengrub.proto.foods.UpdateFoodStatusRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FoodMapperTest {

    private FoodRequestDTO sampleDto() {
        return FoodRequestDTO.builder()
                .id("food-001")
                .foodName("Rice")
                .quantity(QuantityDTO.builder().amount(new BigDecimal("5.0")).unit(Unit.KG).build())
                .requestedBy("user-001")
                .requestedDate(LocalDateTime.of(2024, 1, 10, 9, 0))
                .usedByDate(LocalDateTime.of(2024, 3, 1, 0, 0))
                .status(FoodStatus.PENDING)
                .imageIds(List.of())
                .images(List.of())
                .createdAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .build();
    }

    // ── toProtoFood ───────────────────────────────────────────────────────────

    @Test
    void toProtoFood_mapsAllFields() {
        Food proto = FoodMapper.toProtoFood(sampleDto());

        assertThat(proto.getId()).isEqualTo("food-001");
        assertThat(proto.getFoodName()).isEqualTo("Rice");
        assertThat(proto.getRequestedBy()).isEqualTo("user-001");
        assertThat(proto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void toProtoFood_quantityMapsCorrectly() {
        Food proto = FoodMapper.toProtoFood(sampleDto());

        assertThat(proto.getQuantity().getAmount()).isEqualTo(5.0);
        assertThat(proto.getQuantity().getUnit()).isEqualTo(com.greengrub.proto.foods.Unit.KG);
    }

    @Test
    void toProtoFood_imagesIncludedWhenPresent() {
        FoodRequestDTO dto = sampleDto();
        dto.setImages(List.of(
                ImageDTO.builder().id("img-1").url("https://cdn.example.com/img-1").build()
        ));

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getImagesCount()).isEqualTo(1);
        assertThat(proto.getImages(0).getId()).isEqualTo("img-1");
        assertThat(proto.getImages(0).getUrl()).isEqualTo("https://cdn.example.com/img-1");
    }

    @Test
    void toProtoFood_nullImages_resultsInEmptyList() {
        FoodRequestDTO dto = sampleDto();
        dto.setImages(null);

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getImagesCount()).isEqualTo(0);
    }

    @Test
    void toProtoFood_nullId_safeString() {
        FoodRequestDTO dto = sampleDto();
        dto.setId(null);

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getId()).isEmpty();
    }

    @Test
    void toProtoFood_nullStatus_emptyStatusString() {
        FoodRequestDTO dto = sampleDto();
        dto.setStatus(null);

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getStatus()).isEmpty();
    }

    @Test
    void toProtoFood_servingsUnit() {
        FoodRequestDTO dto = sampleDto();
        dto.setQuantity(QuantityDTO.builder().amount(new BigDecimal("10")).unit(Unit.SERVINGS).build());

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getQuantity().getUnit()).isEqualTo(com.greengrub.proto.foods.Unit.SERVINGS);
    }

    @Test
    void toProtoFood_nullQuantity_defaultsToEmpty() {
        FoodRequestDTO dto = sampleDto();
        dto.setQuantity(null);

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getQuantity()).isNotNull();
    }

    @Test
    void toProtoFood_imageWithContentTypeAndFileName() {
        FoodRequestDTO dto = sampleDto();
        dto.setImages(List.of(
                ImageDTO.builder().id("img-2").url("https://cdn.example.com/img-2")
                        .fileName("photo.jpg").contentType("image/jpeg").build()
        ));

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getImages(0).getFileName()).isEqualTo("photo.jpg");
        assertThat(proto.getImages(0).getContentType()).isEqualTo("image/jpeg");
    }

    // ── toUpdateStatusDto ─────────────────────────────────────────────────────

    @Test
    void toUpdateStatusDto_mapsApproved() {
        UpdateFoodStatusRequest req = UpdateFoodStatusRequest.newBuilder()
                .setFoodId("food-001").setStatus("APPROVED").build();

        assertThat(FoodMapper.toUpdateStatusDto(req).getStatus()).isEqualTo(FoodStatus.APPROVED);
    }

    @Test
    void toUpdateStatusDto_mapsDonated() {
        UpdateFoodStatusRequest req = UpdateFoodStatusRequest.newBuilder()
                .setFoodId("food-001").setStatus("DONATED").build();

        assertThat(FoodMapper.toUpdateStatusDto(req).getStatus()).isEqualTo(FoodStatus.DONATED);
    }

    @Test
    void toUpdateStatusDto_invalidStatus_throwsIllegalArgument() {
        UpdateFoodStatusRequest req = UpdateFoodStatusRequest.newBuilder()
                .setFoodId("food-001").setStatus("NONEXISTENT").build();

        assertThatThrownBy(() -> FoodMapper.toUpdateStatusDto(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── toCreateDto ───────────────────────────────────────────────────────────

    @Test
    void toCreateDto_mapsBasicFields() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Beans")
                .setRequestedBy("user-002")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(3.0).setUnit(com.greengrub.proto.foods.Unit.KG).build())
                .build();

        var dto = FoodMapper.toCreateDto(req);
        assertThat(dto.getFoodName()).isEqualTo("Beans");
        assertThat(dto.getRequestedBy()).isEqualTo("user-002");
        assertThat(dto.getQuantity().getAmount()).isEqualByComparingTo("3.0");
        assertThat(dto.getQuantity().getUnit()).isEqualTo(Unit.KG);
    }

    @Test
    void toCreateDto_emptyUsedByDate_returnsNullDateTime() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Corn")
                .setRequestedBy("user-003")
                .setUsedByDate("")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(1.0).setUnit(com.greengrub.proto.foods.Unit.SERVINGS).build())
                .build();

        assertThat(FoodMapper.toCreateDto(req).getUsedByDate()).isNull();
    }

    @Test
    void toCreateDto_validUsedByDate_parsesCorrectly() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Wheat")
                .setRequestedBy("user-004")
                .setUsedByDate("2024-06-01T00:00:00")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(2.0).setUnit(com.greengrub.proto.foods.Unit.KG).build())
                .build();

        assertThat(FoodMapper.toCreateDto(req).getUsedByDate())
                .isEqualTo(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
    }

    @Test
    void toCreateDto_servingsUnit_mapsCorrectly() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Soup")
                .setRequestedBy("user-005")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(10.0).setUnit(com.greengrub.proto.foods.Unit.SERVINGS).build())
                .build();

        assertThat(FoodMapper.toCreateDto(req).getQuantity().getUnit()).isEqualTo(Unit.SERVINGS);
    }

    // ── edge cases for private helpers ────────────────────────────────────────

    @Test
    void toCreateDto_withImageData_mapsImageBytes() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Apple")
                .setRequestedBy("user-006")
                .addImageData(ByteString.copyFrom(new byte[]{1, 2, 3}))
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(1.0).setUnit(com.greengrub.proto.foods.Unit.KG).build())
                .build();

        var dto = FoodMapper.toCreateDto(req);
        assertThat(dto.getImageData()).hasSize(1);
        assertThat(dto.getImageData().get(0)).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void toProtoFood_nullQuantityAmount_defaultsToZero() {
        FoodRequestDTO dto = sampleDto();
        dto.setQuantity(QuantityDTO.builder().amount(null).unit(Unit.KG).build());

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getQuantity().getAmount()).isEqualTo(0.0);
    }

    @Test
    void toProtoFood_nullUnitInQuantity_usesUnspecified() {
        FoodRequestDTO dto = sampleDto();
        dto.setQuantity(QuantityDTO.builder().amount(new BigDecimal("1.0")).unit(null).build());

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getQuantity().getUnit()).isEqualTo(com.greengrub.proto.foods.Unit.UNIT_UNSPECIFIED);
    }

    @Test
    void toProtoFood_nullDates_formatsAsEmpty() {
        FoodRequestDTO dto = sampleDto();
        dto.setRequestedDate(null);
        dto.setUsedByDate(null);

        Food proto = FoodMapper.toProtoFood(dto);
        assertThat(proto.getRequestedDate()).isEmpty();
        assertThat(proto.getUsedByDate()).isEmpty();
    }

    @Test
    void toCreateDto_invalidUsedByDate_returnsNull() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("X")
                .setRequestedBy("u")
                .setUsedByDate("not-a-date")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(1.0).setUnit(com.greengrub.proto.foods.Unit.KG).build())
                .build();

        assertThat(FoodMapper.toCreateDto(req).getUsedByDate()).isNull();
    }

    @Test
    void toDomainUnit_unspecified_defaultsToServings() {
        com.greengrub.proto.foods.CreateFoodRequest req = com.greengrub.proto.foods.CreateFoodRequest.newBuilder()
                .setFoodName("Z")
                .setRequestedBy("u")
                .setQuantity(com.greengrub.proto.foods.Quantity.newBuilder()
                        .setAmount(1.0).setUnit(com.greengrub.proto.foods.Unit.UNIT_UNSPECIFIED).build())
                .build();

        assertThat(FoodMapper.toCreateDto(req).getQuantity().getUnit()).isEqualTo(Unit.SERVINGS);
    }
}
