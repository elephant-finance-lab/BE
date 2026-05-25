package com.example.elephantfinancelab_be.domain.autotrading.dto.event;

import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AutoTradingKafkaEvent(
    String eventId,
    String syntheticEventId,
    AutoTradingEventType eventType,
    String rawEventType,
    String aiSessionId,
    String requestId,
    String beSessionId,
    String correlationId,
    String idempotencyKey,
    String bundleId,
    String messageKey,
    String payloadJson,
    String rawEventJson,
    JsonNode payload,
    LocalDateTime occurredAt) {}
