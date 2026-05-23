package com.greengrub.food_request.interceptor;

import com.greengrub.food_request.exception.FoodNotFoundException;
import com.greengrub.food_request.exception.ImageUploadFailedException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@GrpcGlobalServerInterceptor
public class GrpcExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    handleException(e, call, headers);
                }
            }

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    handleException(e, call, headers);
                }
            }
        };
    }

    private <ReqT, RespT> void handleException(Exception e, ServerCall<ReqT, RespT> call, Metadata headers) {
        Status status = mapToGrpcStatus(e);
        log.error("[gRPC] {} on method {}: {}",
                status.getCode(), call.getMethodDescriptor().getFullMethodName(), e.getMessage(), e);
        call.close(status, headers);
    }

    private Status mapToGrpcStatus(Exception e) {
        if (e instanceof FoodNotFoundException ex) {
            return Status.NOT_FOUND.withDescription(ex.getMessage());
        }
        if (e instanceof ConstraintViolationException ex) {
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
        }
        if (e instanceof MethodArgumentNotValidException ex) {
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
        }
        if (e instanceof IllegalArgumentException ex) {
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
        }
        if (e instanceof ImageUploadFailedException ex) {
            return Status.UNAVAILABLE.withDescription(ex.getMessage());
        }
        if (e instanceof CallNotPermittedException) {
            return Status.UNAVAILABLE.withDescription("Service temporarily unavailable - circuit breaker open");
        }
        return Status.INTERNAL.withDescription("Internal server error: " + e.getMessage());
    }
}
