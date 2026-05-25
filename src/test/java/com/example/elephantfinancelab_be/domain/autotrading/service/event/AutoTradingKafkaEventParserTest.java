package com.example.elephantfinancelab_be.domain.autotrading.service.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.AutoTradingKafkaEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AutoTradingKafkaEventParserTest {

  private final AutoTradingKafkaEventParser parser =
      new AutoTradingKafkaEventParser(new ObjectMapper());

  @Test
  void parsesCurrentAiFlattenedEnvelopeAndUsesStableSyntheticId() {
    String raw =
        """
        {
          "event_type": "PAPER_ORDER_SUBMITTED",
          "session_id": "ai-session-1",
          "bundle_id": "BUNDLE-1",
          "timestamp": "2026-05-25T12:00:00+09:00",
          "ticker": "005930",
          "side": "BUY"
        }
        """;

    AutoTradingKafkaEvent first = parser.parse("ai-session-1", raw);
    AutoTradingKafkaEvent redelivered = parser.parse("ai-session-1", raw);

    assertThat(first.eventType()).isEqualTo(AutoTradingEventType.PAPER_ORDER_SUBMITTED);
    assertThat(first.aiSessionId()).isEqualTo("ai-session-1");
    assertThat(first.payload().get("ticker").asText()).isEqualTo("005930");
    assertThat(first.syntheticEventId()).isEqualTo(redelivered.syntheticEventId());
  }

  @Test
  void parsesCamelCaseEnvelopeWithNestedPayload() {
    AutoTradingKafkaEvent event =
        parser.parse(
            "message-key",
            """
            {
              "eventId": "EVENT-1",
              "eventType": "PAPER_ORDER_FILLED",
              "sessionId": "ai-session-1",
              "requestId": "start-request-1",
              "occurredAt": "2026-05-25T12:00:00",
              "payload": {"tickerCode": "005930", "filledQuantity": 1}
            }
            """);

    assertThat(event.eventId()).isEqualTo("EVENT-1");
    assertThat(event.eventType()).isEqualTo(AutoTradingEventType.PAPER_ORDER_FILLED);
    assertThat(event.requestId()).isEqualTo("start-request-1");
    assertThat(event.payload().get("filledQuantity").asLong()).isEqualTo(1L);
    assertThat(event.occurredAt()).isNotNull();
  }

  @Test
  void parsesEnhancedAiSnakeCaseEnvelopeWithNestedPaperPayload() {
    AutoTradingKafkaEvent event =
        parser.parse(
            "ai-session-2",
            """
            {
              "event_id": "event-2",
              "event_type": "PAPER_ORDER_SUBMITTED",
              "session_id": "ai-session-2",
              "request_id": "start-request-2",
              "bundle_id": "BUNDLE-2",
              "timestamp": "2026-05-25T12:00:00+09:00",
              "payload": {
                "ticker": "005930",
                "quantity": 1,
                "paper_only": true
              }
            }
            """);

    assertThat(event.eventId()).isEqualTo("event-2");
    assertThat(event.requestId()).isEqualTo("start-request-2");
    assertThat(event.payload().get("paper_only").booleanValue()).isTrue();
    assertThat(event.payloadJson()).doesNotContain("request_id");
  }
}
