package com.example.elephantfinancelab_be.domain.user.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class AccountException extends GeneralException {
  public AccountException(BaseErrorCode code) {
    super(code);
  }
}
