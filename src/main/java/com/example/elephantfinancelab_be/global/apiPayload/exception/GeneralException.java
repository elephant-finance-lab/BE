package com.example.elephantfinancelab_be.global.apiPayload.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

  private final BaseErrorCode code;
  private final String clientMessage;

  public GeneralException(BaseErrorCode code) {
    this(code, null);
  }

  public GeneralException(BaseErrorCode code, String clientMessage) {
    super(clientMessage == null || clientMessage.isBlank() ? code.getMessage() : clientMessage);
    this.code = code;
    this.clientMessage =
        clientMessage == null || clientMessage.isBlank() ? code.getMessage() : clientMessage;
  }
}
