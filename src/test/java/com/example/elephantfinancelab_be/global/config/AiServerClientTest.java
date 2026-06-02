package com.example.elephantfinancelab_be.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

class AiServerClientTest {

  @Test
  void sanitizeAiDetailRedactsSecretsAndCapsLength() {
    String raw =
        "PAPER_START_GATE_BLOCKED token=abc123 app_secret=supersecret "
            + "Authorization=Bearer-secret account=12345678 reason=broker_evidence_not_pass "
            + "payload="
            + "x".repeat(600);

    String sanitized = AiServerClient.sanitizeAiDetail(raw);

    assertThat(sanitized).contains("PAPER_START_GATE_BLOCKED");
    assertThat(sanitized).contains("token=<redacted>");
    assertThat(sanitized).contains("app_secret=<redacted>");
    assertThat(sanitized).contains("Authorization=<redacted>");
    assertThat(sanitized).contains("account=<redacted>");
    assertThat(sanitized).doesNotContain("abc123", "supersecret", "12345678");
    assertThat(sanitized).hasSizeLessThanOrEqualTo(503);
    assertThat(sanitized).endsWith("...");
  }

  @Test
  void sanitizeAiDetailRedactsJsonSecretsAccountAliasesAndLocalPaths() {
    String raw =
        "{\"token\":\"json-token\",\"api_key\":\"json-api-key\"} "
            + "accountNumber='12345678' accountNo=87654321 "
            + "path=/Users/jangjaewon/Desktop/Full_Part/Elephant_Lab/.env "
            + "linux=/home/ubuntu/app/.env "
            + "windows=C:\\Users\\jaewon\\secret\\.env "
            + "unc=\\\\server\\share\\secret\\.env";

    String sanitized = AiServerClient.sanitizeAiDetail(raw);

    assertThat(sanitized).contains("\"token\":\"<redacted>\"");
    assertThat(sanitized).contains("\"api_key\":\"<redacted>\"");
    assertThat(sanitized).contains("accountNumber=<redacted>");
    assertThat(sanitized).contains("accountNo=<redacted>");
    assertThat(sanitized).contains("<local-path-redacted>");
    assertThat(sanitized)
        .doesNotContain(
            "json-token",
            "json-api-key",
            "12345678",
            "87654321",
            "/Users/jangjaewon",
            "/home/ubuntu",
            "C:\\Users\\jaewon",
            "\\\\server\\share");
  }

  @Test
  void mapToAiServerExceptionPreservesSanitizedFailedPreconditionDetail() {
    AiServerClient client = new AiServerClient(null, 1);

    AiServerException exception =
        client.mapToAiServerException(
            Status.FAILED_PRECONDITION
                .withDescription("broker_evidence_not_pass token=abc123")
                .asRuntimeException());

    assertThat(exception.getCode()).isEqualTo(AiServerErrorCode.AI412_01);
    assertThat(exception.getGrpcStatusCode()).isEqualTo("FAILED_PRECONDITION");
    assertThat(exception.getAiDetail()).contains("broker_evidence_not_pass");
    assertThat(exception.getAiDetail()).contains("token=<redacted>");
    assertThat(exception.getAiDetail()).doesNotContain("abc123");
    assertThat(exception.getClientMessage()).contains("broker_evidence_not_pass");
  }

  @Test
  void mapToAiServerExceptionKeepsGrpcStatusSpecificErrorCodes() {
    AiServerClient client = new AiServerClient(null, 1);

    assertThat(
            client
                .mapToAiServerException(
                    Status.INVALID_ARGUMENT
                        .withDescription("confirm_phrase_mismatch")
                        .asRuntimeException())
                .getCode())
        .isEqualTo(AiServerErrorCode.AI400_01);
    assertThat(
            client
                .mapToAiServerException(
                    Status.DEADLINE_EXCEEDED.withDescription("deadline").asRuntimeException())
                .getCode())
        .isEqualTo(AiServerErrorCode.AI504_01);
    assertThat(
            client
                .mapToAiServerException(
                    Status.UNAVAILABLE.withDescription("unavailable").asRuntimeException())
                .getCode())
        .isEqualTo(AiServerErrorCode.AI503_01);
  }

  @Test
  void sanitizeAiDetailReturnsNullForBlankDescription() {
    assertThat(AiServerClient.sanitizeAiDetail(null)).isNull();
    assertThat(AiServerClient.sanitizeAiDetail(" ")).isNull();
  }
}
