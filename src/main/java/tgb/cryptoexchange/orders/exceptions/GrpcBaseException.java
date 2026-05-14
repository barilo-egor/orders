package tgb.cryptoexchange.orders.exceptions;

import com.google.rpc.Code;
import com.google.rpc.Status;
import lombok.Getter;

@Getter
public class GrpcBaseException extends RuntimeException {

    private final Status rpcStatus;

    public GrpcBaseException(Status rpcStatus) {
        super(rpcStatus.getMessage());
        this.rpcStatus = rpcStatus;
    }

    public GrpcBaseException(Code code, String message,  com.google.protobuf.Any... details) {
        super(message);
        this.rpcStatus = Status.newBuilder()
                .setCode(code.getNumber())
                .setMessage(message)
                .addAllDetails(java.util.Arrays.asList(details))
                .build();
    }

}
