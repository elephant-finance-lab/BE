package com.example.elephantfinancelab_be.domain.autotrading.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "auto_trading_events",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_auto_trading_event_external_id", columnNames = "event_id"),
      @UniqueConstraint(
          name = "uk_auto_trading_event_synthetic_id",
          columnNames = "synthetic_event_id")
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoTradingEvent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", length = 160)
  private String eventId;

  @Column(name = "synthetic_event_id", nullable = false, length = 80)
  private String syntheticEventId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private AutoTradingEventType eventType;

  @Column(name = "be_session_id", length = 36)
  private String beSessionId;

  @Column(name = "ai_session_id", length = 100)
  private String aiSessionId;

  @Column(name = "request_id", length = 100)
  private String requestId;

  @Column(name = "idempotency_key", length = 120)
  private String idempotencyKey;

  @Column(name = "bundle_id", length = 160)
  private String bundleId;

  @Column(name = "message_key", length = 160)
  private String messageKey;

  @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
  private String payloadJson;

  @Column(name = "raw_event_json", nullable = false, columnDefinition = "TEXT")
  private String rawEventJson;

  @Column(name = "occurred_at")
  private LocalDateTime occurredAt;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;
}
