package com.example.elephantfinancelab_be.global.apiPayload.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.util.AiDetailSanitizer;
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
    this(code, grpcStatusCode, AiDetailSanitizer.sanitize(aiDetail), true);
  }

  private AiServerException(
      AiServerErrorCode code, String grpcStatusCode, String sanitizedAiDetail, boolean sanitized) {
    super(buildClientMessage(code, sanitizedAiDetail));
    this.code = code;
    this.grpcStatusCode = grpcStatusCode;
    this.aiDetail = sanitizedAiDetail;
    this.clientMessage = buildClientMessage(code, sanitizedAiDetail);
  }

  private static String buildClientMessage(AiServerErrorCode code, String aiDetail) {
    if (aiDetail == null || aiDetail.isBlank()) {
      return code.getMessage();
    }
    return code.getMessage() + " 사유: " + aiDetail;
  }
}
