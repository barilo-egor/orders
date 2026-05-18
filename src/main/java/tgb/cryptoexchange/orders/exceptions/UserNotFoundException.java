package tgb.cryptoexchange.orders.exceptions;

import lombok.Getter;
import tgb.cryptoexchange.orders.enums.ErrorCode;

@Getter
public class UserNotFoundException extends RuntimeException implements CustomException {

    private final ErrorCode errorCode;

    private final String field;

    private final String description;

    public UserNotFoundException() {
        super("User not found.");
        this.errorCode = ErrorCode.NOT_FOUND;
        this.field = null;
        this.description = null;
    }

}
