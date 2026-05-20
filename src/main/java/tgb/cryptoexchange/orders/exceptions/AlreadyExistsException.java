package tgb.cryptoexchange.orders.exceptions;

import lombok.Getter;
import tgb.cryptoexchange.orders.enums.ErrorCode;

@Getter
public class AlreadyExistsException extends RuntimeException implements CustomException{

    private final ErrorCode errorCode;

    private final String field;

    private final String description;

    public AlreadyExistsException(final String field) {
        super("Bad request.");
        this.errorCode = ErrorCode.INVALID_ARGUMENT;
        this.field = field;
        this.description = "Should be unique.";
    }

}
