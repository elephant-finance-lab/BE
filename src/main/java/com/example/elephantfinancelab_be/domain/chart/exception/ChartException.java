package com.example.elephantfinancelab_be.domain.chart.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class ChartException extends GeneralException {

  public ChartException(BaseErrorCode code) {
    super(code);
  }

  public ChartException(BaseErrorCode code, Throwable cause) {
    super(code);
    initCause(cause);
  }

  @Override
  public String getMessage() {
    return getCode().getMessage();
  }
}
