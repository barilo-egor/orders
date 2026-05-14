package tgb.cryptoexchange.orders.controller.handler;

import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;
import tgb.cryptoexchange.orders.exceptions.AlreadyExistsException;
import tgb.cryptoexchange.orders.exceptions.GrpcBaseException;

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

    private void handle(Exception ex, ServerCall<?, ?> call) {
        StatusRuntimeException out;
        switch (ex) {
        case AlreadyExistsException alreadyExistsException ->
                out = buildBadRequestStatus(alreadyExistsException.getField(), ex.getMessage());
        case GrpcBaseException grpcEx -> out = StatusProto.toStatusRuntimeException(grpcEx.getRpcStatus());
        case null, default -> {
            log.error("Unexpected system error: ", ex);
            out = Status.INTERNAL
                    .withDescription("Internal server error")
                    .asRuntimeException();
        }
        }
        call.close(out.getStatus(), out.getTrailers());
    }

    private StatusRuntimeException buildBadRequestStatus(String field, String description) {
        com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                .setCode(com.google.rpc.Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Bad request")
                .addDetails(com.google.protobuf.Any.pack(
                        com.google.rpc.BadRequest.newBuilder()
                                .addFieldViolations(com.google.rpc.BadRequest.FieldViolation.newBuilder()
                                        .setField(field)
                                        .setDescription(description)
                                        .build())
                                .build()
                ))
                .build();
        return StatusProto.toStatusRuntimeException(status);
    }

}
