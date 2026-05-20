package tgb.cryptoexchange.orders.exceptions;

import lombok.Getter;
import tgb.cryptoexchange.orders.enums.ErrorCode;

@Getter
public class NotFoundException extends RuntimeException implements CustomException{

    private final ErrorCode errorCode;

    private final String field;

    private final String description;

    public NotFoundException(final String fieldId) {
        super("Record not found for the provided ID.");
        this.errorCode = ErrorCode.NOT_FOUND;
        this.field = fieldId;
        this.description = null;
    }

}
