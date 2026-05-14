package tgb.cryptoexchange.orders.exceptions;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException{

    private final String fieldId;

    public NotFoundException(final String fieldId) {
        super("Record not found for the provided ID.");
        this.fieldId = fieldId;
    }

}
