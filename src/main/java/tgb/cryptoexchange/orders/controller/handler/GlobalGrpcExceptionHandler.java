package tgb.cryptoexchange.orders.controller.handler;

import com.google.rpc.Code;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;
import tgb.cryptoexchange.orders.enums.ErrorCode;
import tgb.cryptoexchange.orders.exceptions.AlreadyExistsException;
import tgb.cryptoexchange.orders.exceptions.CustomException;
import tgb.cryptoexchange.orders.exceptions.GrpcValidationException;

@Slf4j
@Component
@GlobalServerInterceptor
public class GlobalGrpcExceptionHandler implements ServerInterceptor {

    @Override
    public <T, O> ServerCall.Listener<T> interceptCall(
            ServerCall<T, O> call,
            Metadata headers,
            ServerCallHandler<T, O> next) {

        ServerCall.Listener<T> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(T message) {
                try {
                    super.onMessage(message);
                } catch (Exception ex) {
                    handle(ex, call);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception ex) {
                    handle(ex, call);
                }
            }
        };
    }

    private com.google.rpc.Code determineGrpcCode(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_ARGUMENT -> com.google.rpc.Code.INVALID_ARGUMENT;
            case NOT_FOUND -> com.google.rpc.Code.NOT_FOUND;
            case INTERNAL -> Code.INTERNAL;
        };
    }

    private void handle(Exception ex, ServerCall<?, ?> call) {
        StatusRuntimeException out;
        switch (ex) {
        case CustomException customEx -> {
            com.google.rpc.Code grpcCode = determineGrpcCode(customEx.getErrorCode());
            out = buildStatus(grpcCode, ex.getMessage(), customEx.getField(), customEx.getDescription());

        }
        case GrpcValidationException grpcEx -> out = StatusProto.toStatusRuntimeException(grpcEx.getRpcStatus());
        case null, default -> {
            log.error("Unexpected system error: ", ex);
            out = Status.INTERNAL
                    .withDescription("Internal server error")
                    .asRuntimeException();
        }
        }
        Metadata trailers = out.getTrailers();
        if (trailers == null) {
            trailers = new Metadata();
        }
        call.close(out.getStatus(), trailers);
    }

    private StatusRuntimeException buildStatus(com.google.rpc.Code code, String message,
            String field, String description) {
        com.google.rpc.Status.Builder statusBuilder = com.google.rpc.Status.newBuilder()
                .setCode(code.getNumber())
                .setMessage(message);

        if (field != null && description != null) {
            com.google.rpc.BadRequest badRequest = com.google.rpc.BadRequest.newBuilder()
                    .addFieldViolations(com.google.rpc.BadRequest.FieldViolation.newBuilder()
                            .setField(field)
                            .setDescription(description)
                            .build())
                    .build();
            statusBuilder.addDetails(com.google.protobuf.Any.pack(badRequest));
        }
        return StatusProto.toStatusRuntimeException(statusBuilder.build());
    }

}
