package com.example.elephantfinancelab_be.global.apiPayload.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import lombok.Getter;

@Getter
public class AiServerException extends RuntimeException {

  private final AiServerErrorCode code;
  private final String grpcStatusCode;
  private final String aiDetail;
  private final String clientMessage;

  public AiServerException(AiServerErrorCode code) {
    this(code, null, null);
  }

  public AiServerException(AiServerErrorCode code, String grpcStatusCode, String aiDetail) {
    super(buildClientMessage(code, aiDetail));
    this.code = code;
    this.grpcStatusCode = grpcStatusCode;
    this.aiDetail = aiDetail;
    this.clientMessage = buildClientMessage(code, aiDetail);
  }

  private static String buildClientMessage(AiServerErrorCode code, String aiDetail) {
    if (aiDetail == null || aiDetail.isBlank()) {
      return code.getMessage();
    }
    return code.getMessage() + " 사유: " + aiDetail;
  }
}
