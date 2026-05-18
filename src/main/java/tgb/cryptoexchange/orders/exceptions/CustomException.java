package tgb.cryptoexchange.orders.exceptions;

import tgb.cryptoexchange.orders.enums.ErrorCode;

public interface CustomException {

    ErrorCode getErrorCode();

    String getField();

    String getDescription();

}
