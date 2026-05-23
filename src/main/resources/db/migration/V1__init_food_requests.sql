-- V1__init_food_requests.sql
-- Owns the food-service schema from inception. Hibernate is configured with
-- ddl-auto=validate, so any drift between this file and the JPA entities will
-- fail at startup rather than silently mutate the database.

CREATE TABLE food_requests (
    id              VARCHAR(36)     PRIMARY KEY,
    food_name       VARCHAR(255)    NOT NULL,
    quantity_amount NUMERIC(12, 2)  NOT NULL,
    quantity_unit   VARCHAR(32)     NOT NULL,
    requested_by    VARCHAR(36)     NOT NULL,
    requested_date  TIMESTAMP       NOT NULL,
    used_by_date    TIMESTAMP,
    status          VARCHAR(32)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_food_requests_requested_by ON food_requests (requested_by);
CREATE INDEX idx_food_requests_status        ON food_requests (status);

-- @ElementCollection mapping for List<String> imageIds.
-- ON DELETE CASCADE so removing a food row drops its image-id rows automatically.
CREATE TABLE food_request_image_ids (
    food_request_id VARCHAR(36) NOT NULL,
    image_id        VARCHAR(36) NOT NULL,
    PRIMARY KEY (food_request_id, image_id),
    CONSTRAINT fk_food_request_image FOREIGN KEY (food_request_id)
        REFERENCES food_requests (id) ON DELETE CASCADE
);
