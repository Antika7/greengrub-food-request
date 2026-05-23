package com.greengrub.food_request.mapper;

import com.greengrub.food_request.dto.CreateFoodRequestDTO;
import com.greengrub.food_request.dto.FoodRequestDTO;
import com.greengrub.food_request.dto.ImageDTO;
import com.greengrub.food_request.dto.QuantityDTO;
import com.greengrub.food_request.dto.UpdateFoodStatusDTO;
import com.greengrub.food_request.entity.FoodStatus;
import com.greengrub.food_request.entity.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bridges the wire (proto) and the domain (DTO/entity). Kept as static helpers
 * because the mapping is straightforward and test setup is easier without DI.
 */
public final class FoodMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private FoodMapper() {}

    // ---- proto → DTO (request side) ----

    public static CreateFoodRequestDTO toCreateDto(com.greengrub.proto.foods.CreateFoodRequest req) {
        return CreateFoodRequestDTO.builder()
                .foodName(req.getFoodName())
                .quantity(toDtoQuantity(req.getQuantity()))
                .requestedBy(req.getRequestedBy())
                .usedByDate(parseIso(req.getUsedByDate()))
                .imageData(req.getImageDataList().stream().map(bs -> bs.toByteArray()).toList())
                .build();
    }

    public static UpdateFoodStatusDTO toUpdateStatusDto(com.greengrub.proto.foods.UpdateFoodStatusRequest req) {
        return UpdateFoodStatusDTO.builder()
                .status(FoodStatus.valueOf(req.getStatus()))
                .build();
    }

    // ---- DTO → proto (response side) ----

    public static com.greengrub.proto.foods.Food toProtoFood(FoodRequestDTO dto) {
        com.greengrub.proto.foods.Food.Builder b = com.greengrub.proto.foods.Food.newBuilder()
                .setId(safe(dto.getId()))
                .setFoodName(safe(dto.getFoodName()))
                .setQuantity(toProtoQuantity(dto.getQuantity()))
                .setRequestedBy(safe(dto.getRequestedBy()))
                .setRequestedDate(formatIso(dto.getRequestedDate()))
                .setUsedByDate(formatIso(dto.getUsedByDate()))
                .setStatus(dto.getStatus() != null ? dto.getStatus().name() : "")
                .setCreatedAt(formatIso(dto.getCreatedAt()))
                .setUpdatedAt(formatIso(dto.getUpdatedAt()));

        if (dto.getImages() != null) {
            for (ImageDTO image : dto.getImages()) {
                b.addImages(toProtoImage(image));
            }
        }
        return b.build();
    }

    private static com.greengrub.proto.foods.Image toProtoImage(ImageDTO image) {
        com.greengrub.proto.foods.Image.Builder b = com.greengrub.proto.foods.Image.newBuilder()
                .setId(safe(image.getId()))
                .setUrl(safe(image.getUrl()));
        if (image.getFileName() != null) {
            b.setFileName(image.getFileName());
        }
        if (image.getContentType() != null) {
            b.setContentType(image.getContentType());
        }
        return b.build();
    }

    // ---- proto.Quantity ↔ QuantityDTO ----

    private static QuantityDTO toDtoQuantity(com.greengrub.proto.foods.Quantity proto) {
        return QuantityDTO.builder()
                .amount(BigDecimal.valueOf(proto.getAmount()))
                .unit(toDomainUnit(proto.getUnit()))
                .build();
    }

    private static com.greengrub.proto.foods.Quantity toProtoQuantity(QuantityDTO dto) {
        if (dto == null) {
            return com.greengrub.proto.foods.Quantity.getDefaultInstance();
        }
        return com.greengrub.proto.foods.Quantity.newBuilder()
                .setAmount(dto.getAmount() != null ? dto.getAmount().doubleValue() : 0.0)
                .setUnit(toProtoUnit(dto.getUnit()))
                .build();
    }

    private static Unit toDomainUnit(com.greengrub.proto.foods.Unit proto) {
        return switch (proto) {
            case KG -> Unit.KG;
            case SERVINGS -> Unit.SERVINGS;
            default -> Unit.SERVINGS;
        };
    }

    private static com.greengrub.proto.foods.Unit toProtoUnit(Unit unit) {
        if (unit == null) return com.greengrub.proto.foods.Unit.UNIT_UNSPECIFIED;
        return switch (unit) {
            case KG -> com.greengrub.proto.foods.Unit.KG;
            case SERVINGS -> com.greengrub.proto.foods.Unit.SERVINGS;
        };
    }

    // ---- ISO helpers ----

    private static LocalDateTime parseIso(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, ISO);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatIso(LocalDateTime value) {
        return value != null ? value.format(ISO) : "";
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
