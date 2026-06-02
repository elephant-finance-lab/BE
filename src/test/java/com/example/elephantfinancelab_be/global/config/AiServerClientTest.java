package com.example.elephantfinancelab_be.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiServerClientTest {

  @Test
  void sanitizeAiDetailRedactsSecretsAndCapsLength() {
    String raw =
        "PAPER_START_GATE_BLOCKED token=abc123 app_secret=supersecret "
            + "Authorization=Bearer-secret account=12345678 reason=broker_evidence_not_pass";

    String sanitized = AiServerClient.sanitizeAiDetail(raw);

    assertThat(sanitized).contains("PAPER_START_GATE_BLOCKED");
    assertThat(sanitized).contains("token=<redacted>");
    assertThat(sanitized).contains("app_secret=<redacted>");
    assertThat(sanitized).contains("Authorization=<redacted>");
    assertThat(sanitized).contains("account=<redacted>");
    assertThat(sanitized).doesNotContain("abc123", "supersecret", "12345678");
  }

  @Test
  void sanitizeAiDetailReturnsNullForBlankDescription() {
    assertThat(AiServerClient.sanitizeAiDetail(null)).isNull();
    assertThat(AiServerClient.sanitizeAiDetail(" ")).isNull();
  }
}
