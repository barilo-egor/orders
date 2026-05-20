package tgb.cryptoexchange.orders.controller.handler;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.validate.Violation;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GlobalServerInterceptor
public class GrpcValidationInterceptor implements ServerInterceptor {

    Validator validator = ValidatorFactory.newBuilder().build();

    @Override
    public <R, T> ServerCall.Listener<R> interceptCall(
            ServerCall<R, T> call, Metadata headers, ServerCallHandler<R, T> next) {
        return new ValidatingServerCallListener<>(call, headers, next);
    }

    private class ValidatingServerCallListener<R, T> extends ServerCall.Listener<R> {

        private final ServerCall<R, T> call;

        private final Metadata headers;

        private final ServerCallHandler<R, T> next;

        private ServerCall.Listener<R> delegate = null;

        private boolean closed = false;

        public ValidatingServerCallListener(ServerCall<R, T> call, Metadata headers, ServerCallHandler<R, T> next) {
            this.call = call;
            this.headers = headers;
            this.next = next;
        }

        @Override
        public void onMessage(R message) {
            if (closed) {
                return;
            }

            if (message instanceof Message protobufMessage && !processValidation(protobufMessage)) {
                return;
            }

            ensureDelegateStarted();
            delegate.onMessage(message);
        }

        @Override
        public void onHalfClose() {
            if (!closed && delegate != null) {
                delegate.onHalfClose();
            }
        }

        @Override
        public void onCancel() {
            if (delegate != null) {
                delegate.onCancel();
            }
        }

        @Override
        public void onComplete() {
            if (delegate != null) {
                delegate.onComplete();
            }
        }

        @Override
        public void onReady() {
            if (delegate != null) {
                delegate.onReady();
            } else {
                call.request(1);
            }
        }

        private boolean processValidation(Message protobufMessage) {
            try {
                ValidationResult result = validator.validate(protobufMessage);
                if (result.isSuccess()) {
                    return true;
                }

                handleValidationViolations(result);
                return false;

            } catch (build.buf.protovalidate.exceptions.ValidationException e) {
                closeWithRpcError(Code.INTERNAL, "Внутренняя ошибка проверки контракта", null);
                return false;
            }
        }

        private void handleValidationViolations(ValidationResult result) {
            BadRequest.Builder badRequestBuilder = BadRequest.newBuilder();
            for (Violation violation : result.toProto().getViolationsList()) {
                badRequestBuilder.addFieldViolations(
                        BadRequest.FieldViolation.newBuilder()
                                .setField(violation.getField().toString())
                                .setDescription(violation.getMessage())
                                .build()
                );
            }

            closeWithRpcError(Code.INVALID_ARGUMENT, "Ошибка валидации входных параметров",
                    Any.pack(badRequestBuilder.build()));
        }

        private void ensureDelegateStarted() {
            if (delegate == null) {
                delegate = next.startCall(call, headers);
            }
        }

        private void closeWithRpcError(Code code, String message, Any details) {
            closed = true;
            com.google.rpc.Status.Builder statusBuilder = com.google.rpc.Status.newBuilder()
                    .setCode(code.getNumber())
                    .setMessage(message);

            if (details != null) {
                statusBuilder.addDetails(details);
            }

            StatusRuntimeException out = StatusProto.toStatusRuntimeException(statusBuilder.build());
            call.close(out.getStatus(), out.getTrailers());
        }

    }

}
