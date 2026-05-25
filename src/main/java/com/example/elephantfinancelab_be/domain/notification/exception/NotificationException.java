package com.example.elephantfinancelab_be.domain.notification.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class NotificationException extends GeneralException {

  public NotificationException(BaseErrorCode code) {
    super(code);
  }
}
