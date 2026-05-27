package com.greengrub.food_request.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExceptionTest {

    // ── FoodNotFoundException ─────────────────────────────────────────────────

    @Test
    void foodNotFoundException_messageContainsId() {
        FoodNotFoundException ex = new FoodNotFoundException("food-123");
        assertThat(ex.getMessage()).isEqualTo("Food request not found with id: food-123");
    }

    @Test
    void foodNotFoundException_isRuntimeException() {
        assertThat(new FoodNotFoundException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void foodNotFoundException_noCause() {
        assertThat(new FoodNotFoundException("y").getCause()).isNull();
    }

    // ── ImageUploadFailedException ────────────────────────────────────────────

    @Test
    void imageUploadFailedException_messageOnly() {
        ImageUploadFailedException ex = new ImageUploadFailedException("upload timed out");
        assertThat(ex.getMessage()).isEqualTo("upload timed out");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void imageUploadFailedException_messageAndCause() {
        Throwable cause = new RuntimeException("gRPC error");
        ImageUploadFailedException ex = new ImageUploadFailedException("upload failed", cause);
        assertThat(ex.getMessage()).isEqualTo("upload failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void imageUploadFailedException_isRuntimeException() {
        assertThat(new ImageUploadFailedException("msg")).isInstanceOf(RuntimeException.class);
    }

    // ── ImageHydrationFailedException ─────────────────────────────────────────

    @Test
    void imageHydrationFailedException_preservesMessageAndCause() {
        Throwable cause = new RuntimeException("timeout");
        ImageHydrationFailedException ex = new ImageHydrationFailedException("hydration failed", cause);
        assertThat(ex.getMessage()).isEqualTo("hydration failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void imageHydrationFailedException_isRuntimeException() {
        assertThat(new ImageHydrationFailedException("msg", new RuntimeException()))
                .isInstanceOf(RuntimeException.class);
    }

    // ── ErrorResponse ─────────────────────────────────────────────────────────

    @Test
    void errorResponse_of_setsAllFields() {
        ErrorResponse resp = ErrorResponse.of(404, "Not Found", "item missing", "/api/v1/food");
        assertThat(resp.getStatus()).isEqualTo(404);
        assertThat(resp.getError()).isEqualTo("Not Found");
        assertThat(resp.getMessage()).isEqualTo("item missing");
        assertThat(resp.getPath()).isEqualTo("/api/v1/food");
        assertThat(resp.getTimestamp()).isNotNull();
    }

    @Test
    void errorResponse_of_500() {
        ErrorResponse resp = ErrorResponse.of(500, "Internal Server Error", "unexpected", "/api");
        assertThat(resp.getStatus()).isEqualTo(500);
    }
}
