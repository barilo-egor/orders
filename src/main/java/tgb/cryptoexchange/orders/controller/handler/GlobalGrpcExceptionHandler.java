package tgb.cryptoexchange.orders.controller.handler;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;
import tgb.cryptoexchange.orders.exceptions.CustomException;
import tgb.cryptoexchange.orders.exceptions.GrpcValidationException;

@Slf4j
@Component
@GlobalServerInterceptor
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public io.grpc.StatusException handleException(Throwable ex) {
        return switch (ex) {
            case CustomException customEx -> {
                com.google.rpc.Code grpcCode = customEx.getErrorCode();
                yield buildStatusException(grpcCode, ex.getMessage(), customEx.getField(), customEx.getDescription());
            }
            case GrpcValidationException grpcEx -> StatusProto.toStatusException(grpcEx.getRpcStatus());
            default -> {
                log.error("Unexpected system error: ", ex);
                yield Status.INTERNAL
                        .withDescription("Internal server error")
                        .asException();
            }
        };
    }

    private StatusException buildStatusException(com.google.rpc.Code code, String message, String field,
            String description) {
        com.google.rpc.Status.Builder statusBuilder = com.google.rpc.Status.newBuilder()
                .setCode(code.getNumber())
                .setMessage(message != null ? message : "");

        if (code == com.google.rpc.Code.INVALID_ARGUMENT) {
            com.google.rpc.BadRequest badRequest = com.google.rpc.BadRequest.newBuilder()
                    .addFieldViolations(com.google.rpc.BadRequest.FieldViolation.newBuilder()
                            .setField(field != null ? field : "")
                            .setDescription(description != null ? description : "")
                            .build())
                    .build();
            statusBuilder.addDetails(com.google.protobuf.Any.pack(badRequest));
        }

        var runtimeEx = StatusProto.toStatusRuntimeException(statusBuilder.build());
        return new StatusException(runtimeEx.getStatus(), runtimeEx.getTrailers());
    }

}
