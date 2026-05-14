package tgb.cryptoexchange.orders.exceptions;

import lombok.Getter;

@Getter
public class AlreadyExistsException extends RuntimeException{

    private final String field;

    public AlreadyExistsException(final String field) {
        super("Should be unique.");
        this.field = field;
    }

}
