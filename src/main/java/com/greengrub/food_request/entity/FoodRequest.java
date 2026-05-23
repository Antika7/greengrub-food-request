package com.greengrub.food_request.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "food_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodRequest {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Embedded
    private Quantity quantity;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @CreationTimestamp
    @Column(name = "requested_date", nullable = false, updatable = false)
    private LocalDateTime requestedDate;

    @Column(name = "used_by_date")
    private LocalDateTime usedByDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FoodStatus status;

    // Image ids returned by image-service after upload. Hydrated on read paths
    // via a single GetImagesByCreator(food_uuid) gRPC call — see ImageServiceClient.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "food_request_image_ids",
            joinColumns = @JoinColumn(name = "food_request_id")
    )
    @Column(name = "image_id", length = 36)
    @Builder.Default
    private List<String> imageIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
