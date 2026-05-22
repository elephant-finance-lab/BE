package com.example.elephantfinancelab_be.global.apiPayload.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import lombok.Getter;

@Getter
public class AiServerException extends RuntimeException {

  private final AiServerErrorCode code;

  public AiServerException(AiServerErrorCode code) {
    super(code.getMessage());
    this.code = code;
  }
}
