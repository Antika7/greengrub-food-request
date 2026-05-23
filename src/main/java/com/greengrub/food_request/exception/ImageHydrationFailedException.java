package com.greengrub.food_request.exception;

/**
 * Thrown only for retry/metric purposes inside the hydration helper. The
 * service-layer catches it and falls back to an empty image list — read paths
 * never propagate this to the client.
 */
public class ImageHydrationFailedException extends RuntimeException {
    public ImageHydrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
