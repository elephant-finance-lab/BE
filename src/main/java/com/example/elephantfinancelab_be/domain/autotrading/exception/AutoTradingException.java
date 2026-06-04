package com.example.elephantfinancelab_be.domain.autotrading.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class AutoTradingException extends GeneralException {

  public AutoTradingException(BaseErrorCode code) {
    super(code);
  }

  public AutoTradingException(BaseErrorCode code, String clientMessage) {
    super(code, clientMessage);
  }
}
