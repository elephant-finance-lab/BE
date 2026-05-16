package com.example.elephantfinancelab_be.domain.portfolio.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class PortfolioException extends GeneralException {

  public PortfolioException(BaseErrorCode code) {
    super(code);
  }

  @Override
  public String getMessage() {
    return getCode().getMessage();
  }
}
