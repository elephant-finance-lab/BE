package com.example.elephantfinancelab_be.global.apiPayload.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.exception.code.AutoTradingErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GeneralExceptionAdviceTest {

  private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

  @Test
  void aiServerExceptionResponsePreservesClientSafeAiDetail() {
    ResponseEntity<ApiResponse<Void>> response =
        advice.handleAiServerException(
            new AiServerException(
                AiServerErrorCode.AI412_01, "FAILED_PRECONDITION", "broker_evidence_not_pass"));

    assertThat(response.getStatusCode()).isEqualTo(AiServerErrorCode.AI412_01.getStatus());
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("AI412_01");
    assertThat(response.getBody().getMessage()).contains("broker_evidence_not_pass");
  }

  @Test
  void aiServerExceptionResponseRedactsRawDetailFromDirectConstructor() {
    ResponseEntity<ApiResponse<Void>> response =
        advice.handleAiServerException(
            new AiServerException(
                AiServerErrorCode.AI412_01,
                "FAILED_PRECONDITION",
                "broker_evidence_not_pass token=raw-token accountNumber=12345678"));

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).contains("broker_evidence_not_pass");
    assertThat(response.getBody().getMessage()).contains("token=<redacted>");
    assertThat(response.getBody().getMessage()).contains("accountNumber=<redacted>");
    assertThat(response.getBody().getMessage()).doesNotContain("raw-token", "12345678");
  }

  @Test
  void generalExceptionResponsePreservesAutoTradingRejectionReason() {
    ResponseEntity<ApiResponse<Void>> response =
        advice.handleGeneralException(
            new AutoTradingException(
                AutoTradingErrorCode.AI_START_REJECTED,
                "PAPER_START_GATE_BLOCKED: broker_evidence_not_pass"));

    assertThat(response.getStatusCode())
        .isEqualTo(AutoTradingErrorCode.AI_START_REJECTED.getStatus());
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("AUTO_TRADING502_01");
    assertThat(response.getBody().getMessage())
        .isEqualTo("PAPER_START_GATE_BLOCKED: broker_evidence_not_pass");
  }
}
