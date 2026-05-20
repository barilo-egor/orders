package tgb.cryptoexchange.orders.exceptions;

import com.google.rpc.Code;
import com.google.rpc.Status;
import lombok.Getter;

@Getter
public class GrpcValidationException extends RuntimeException {

    private final Status rpcStatus;

    public GrpcValidationException(Status rpcStatus) {
        super(rpcStatus.getMessage());
        this.rpcStatus = rpcStatus;
    }

    public GrpcValidationException(Code code, String message, com.google.protobuf.Any... details) {
        super(message);
        this.rpcStatus = Status.newBuilder()
                .setCode(code.getNumber())
                .setMessage(message)
                .addAllDetails(java.util.Arrays.asList(details))
                .build();
    }

}
