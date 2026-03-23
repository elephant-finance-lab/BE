package com.example.elephantfinancelab_be.domain.user.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class UserException extends GeneralException {
  public UserException(BaseErrorCode code) {
    super(code);
  }
}
