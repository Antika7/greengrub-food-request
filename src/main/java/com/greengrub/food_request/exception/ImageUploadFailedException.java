package com.greengrub.food_request.exception;

/**
 * Thrown when image-service rejects an upload (or the gRPC call times out /
 * the breaker is open). Surfaces as 502 to the REST client because the food
 * row depends on the upload completing.
 */
public class ImageUploadFailedException extends RuntimeException {
    public ImageUploadFailedException(String message) {
        super(message);
    }

    public ImageUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
