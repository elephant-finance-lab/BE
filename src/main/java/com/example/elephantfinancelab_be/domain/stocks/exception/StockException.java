package com.example.elephantfinancelab_be.domain.stocks.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class StockException extends GeneralException {

  public StockException(BaseErrorCode code) {
    super(code);
  }

  public StockException(BaseErrorCode code, Throwable cause) {
    super(code);
    initCause(cause);
  }

  @Override
  public String getMessage() {
    return getCode().getMessage();
  }
}
