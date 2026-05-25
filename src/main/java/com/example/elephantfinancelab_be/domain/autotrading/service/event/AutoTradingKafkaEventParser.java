package com.example.elephantfinancelab_be.domain.autotrading.service.event;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.AutoTradingKafkaEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoTradingKafkaEventParser {

  private static final List<String> ENVELOPE_FIELDS =
      List.of(
          "event_id",
          "eventId",
          "event_type",
          "eventType",
          "session_id",
          "sessionId",
          "request_id",
          "requestId",
          "be_session_id",
          "beSessionId",
          "correlation_id",
          "correlationId",
          "idempotency_key",
          "idempotencyKey",
          "bundle_id",
          "bundleId",
          "timestamp",
          "occurred_at",
          "occurredAt",
          "payload");

  private final ObjectMapper objectMapper;

  public AutoTradingKafkaEvent parse(String messageKey, String rawEventJson) {
    String rawValue = rawEventJson == null ? "null" : rawEventJson;
    ObjectNode root = readObject(rawValue);
    String rawType = text(root, "event_type", "eventType");
    JsonNode payload = payload(root);
    String payloadJson = write(payload);
    String eventId = text(root, "event_id", "eventId");
    String occurredRaw = text(root, "timestamp", "occurred_at", "occurredAt");
    String syntheticEventId =
        syntheticId(
            eventId,
            firstNonBlank(text(root, "session_id", "sessionId"), messageKey),
            rawType,
            occurredRaw,
            payloadJson);
    return new AutoTradingKafkaEvent(
        eventId,
        syntheticEventId,
        AutoTradingEventType.from(rawType),
        rawType,
        text(root, "session_id", "sessionId"),
        text(root, "request_id", "requestId"),
        text(root, "be_session_id", "beSessionId"),
        text(root, "correlation_id", "correlationId"),
        text(root, "idempotency_key", "idempotencyKey"),
        text(root, "bundle_id", "bundleId"),
        blankToNull(messageKey),
        payloadJson,
        rawValue,
        payload,
        parseDateTime(occurredRaw));
  }

  private ObjectNode readObject(String rawEventJson) {
    try {
      JsonNode parsed = objectMapper.readTree(rawEventJson);
      if (parsed != null && parsed.isObject()) {
        return (ObjectNode) parsed;
      }
    } catch (JsonProcessingException ignored) {
      // Store malformed input as UNKNOWN instead of losing Kafka evidence.
    }
    return objectMapper.createObjectNode().put("unparseableRawValue", rawEventJson);
  }

  private JsonNode payload(ObjectNode root) {
    JsonNode nested = root.get("payload");
    if (nested != null && nested.isObject()) {
      return nested;
    }
    ObjectNode flattened = root.deepCopy();
    ENVELOPE_FIELDS.forEach(flattened::remove);
    return flattened;
  }

  private String write(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("자동매매 Kafka payload JSON 변환에 실패했습니다.", exception);
    }
  }

  private static String text(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode value = node.get(fieldName);
      if (value != null && !value.isNull() && !value.asText().isBlank()) {
        return value.asText().trim();
      }
    }
    return null;
  }

  private static String syntheticId(
      String eventId,
      String sessionCandidate,
      String eventType,
      String occurredAt,
      String payloadJson) {
    // Without an AI event_id this prevents identical re-deliveries, but cannot distinguish
    // legitimately separate events with identical session/type/timestamp/payload content.
    String fingerprint =
        eventId != null
            ? "external|" + eventId
            : String.join(
                "|",
                valueOrEmpty(sessionCandidate),
                valueOrEmpty(eventType),
                valueOrEmpty(occurredAt),
                payloadJson);
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
      return "sha256:" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest를 사용할 수 없습니다.", exception);
    }
  }

  private static LocalDateTime parseDateTime(String value) {
    if (value == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDateTime.parse(value);
      } catch (DateTimeParseException malformed) {
        return null;
      }
    }
  }

  private static String firstNonBlank(String first, String second) {
    return first != null ? first : blankToNull(second);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }
}
